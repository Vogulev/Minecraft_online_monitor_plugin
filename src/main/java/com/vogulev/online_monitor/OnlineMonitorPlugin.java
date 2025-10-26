package com.vogulev.online_monitor;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;


public class OnlineMonitorPlugin extends JavaPlugin implements Listener
{
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private DatabaseManager database;
    private final Map<String, Long> playerJoinTimes = new HashMap<>();

    @Override
    public void onEnable()
    {
        getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();

        // Инициализация базы данных
        database = new DatabaseManager(getDataFolder());
        database.setPlugin(this); // Устанавливаем ссылку на плагин для async операций
        try {
            // Устанавливаем часовой пояс из конфигурации
            String timezoneOffset = getConfig().getString("timezone-offset", "+3");
            database.setTimezoneOffset(timezoneOffset);

            // Подключение к БД
            String dbType = getConfig().getString("database.type", "sqlite");
            String host = getConfig().getString("database.mysql.host", "localhost");
            int port = getConfig().getInt("database.mysql.port", 3306);
            String dbName = getConfig().getString("database.mysql.database", "minecraft_stats");
            String username = getConfig().getString("database.mysql.username", "root");
            String password = getConfig().getString("database.mysql.password", "password");

            database.connect(dbType, host, port, dbName, username, password);
            logger.info("OnlineMonitor plugin enabled with database!");
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        updateMaxOnline();

        // Периодическая задача для записи снимков онлайна (каждые 5 минут)
        long snapshotInterval = getConfig().getLong("snapshot-interval-minutes", 5) * 60 * 20; // В тиках
        getServer().getScheduler().runTaskTimer(this, () -> {
            int currentOnline = getServer().getOnlinePlayers().size();
            database.recordOnlineSnapshot(currentOnline);
        }, snapshotInterval, snapshotInterval);

        // Очистка старых снимков (каждый день)
        int daysToKeep = getConfig().getInt("snapshot-days-to-keep", 30);
        getServer().getScheduler().runTaskTimer(this, () -> {
            database.cleanOldSnapshots(daysToKeep);
        }, 24000L, 24000L); // Каждые 20 минут (1 игровой день)

        logger.info("Online snapshots will be recorded every " + (snapshotInterval / 1200) + " minutes");
    }

    @Override
    public void onDisable()
    {
        // Закрываем активные сессии при остановке сервера
        for (Map.Entry<String, Long> entry : playerJoinTimes.entrySet()) {
            long sessionDuration = System.currentTimeMillis() - entry.getValue();
            database.recordPlayerQuit(entry.getKey(), sessionDuration);
        }
        playerJoinTimes.clear();

        if (database != null) {
            database.disconnect();
        }

        logger.info("OnlineMonitor plugin disabled!");
    }


    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        String playerName = player.getName();

        updateMaxOnline();

        boolean isFirstTime = !player.hasPlayedBefore();
        if (isFirstTime) {
            database.incrementUniquePlayer();
            logger.info("Новый игрок присоединился: " + player.getName());
        }

        // Записываем заход в БД
        database.recordPlayerJoin(playerName);

        // Сохраняем время захода для текущей сессии
        playerJoinTimes.put(playerName, System.currentTimeMillis());

        String welcomeMessage = getConfig().getString("welcome-message",
                "Добро пожаловать на сервер, %player%! Онлайн: %online%");

        welcomeMessage = welcomeMessage
                .replace("%player%", playerName)
                .replace("%online%", String.valueOf(getServer().getOnlinePlayers().size()));

        welcomeMessage = ChatColor.translateAlternateColorCodes('&', welcomeMessage);

        player.sendMessage(welcomeMessage);

        logger.info(player.getName() + " joined. Online: " + getServer().getOnlinePlayers().size());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String playerName = player.getName();

        Long joinTime = playerJoinTimes.get(playerName);
        if (joinTime != null) {
            long sessionTime = System.currentTimeMillis() - joinTime;
            long minutes = sessionTime / (1000 * 60);

            // Записываем выход в БД
            database.recordPlayerQuit(playerName, sessionTime);

            getLogger().info(playerName + " провел в игре: " + minutes + " минут");

            playerJoinTimes.remove(playerName);
        }

        logger.info(player.getName() + " вышел. Онлайн: " + (getServer().getOnlinePlayers().size() - 1));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("online")) {
            if (args.length == 0) {
                sendBasicStats(sender);
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "stats":
                    sendDetailedStats(sender);
                    break;
                case "top":
                    sendTopStats(sender);
                    break;
                case "player":
                    if (args.length > 1) {
                        sendPlayerStats(sender, args[1]);
                    } else {
                        sendColoredMessage(sender, "&cИспользование: &e/online player <ник>");
                    }
                    break;
                case "hourly":
                    int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                    sendHourlyStats(sender, days);
                    break;
                case "daily":
                    int daysDaily = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                    sendDailyStats(sender, daysDaily);
                    break;
                case "weekday":
                    int weeks = args.length > 1 ? parseIntOrDefault(args[1], 4) : 4;
                    sendWeekdayStats(sender, weeks);
                    break;
                case "peak":
                    int daysPeak = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                    sendPeakHours(sender, daysPeak);
                    break;
                default:
                    sendColoredMessage(sender, "&cНеизвестная команда.");
                    sendColoredMessage(sender, "&7Используйте: &e/online [stats|top|player|hourly|daily|weekday|peak]");
            }
            return true;
        }

        return false;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

        if (!command.getName().equalsIgnoreCase("online")) {
            return null;
        }

        if (args.length == 1) {
            return Stream.of("stats", "top", "player", "hourly", "daily", "weekday", "peak", "help")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }


    private void updateMaxOnline()
    {
        int currentOnline = getServer().getOnlinePlayers().size();
        database.updateMaxOnline(currentOnline);
    }

    private void sendBasicStats(CommandSender sender) {
        int currentOnline = getServer().getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();

        sendColoredMessage(sender, "&6&l=== &eСтатистика онлайна &6&l===");
        sendColoredMessage(sender, "&7Сейчас онлайн: &a" + currentOnline + " &7игроков");
        sendColoredMessage(sender, "&7Максимум онлайна: &b" + maxOnline);
        sendColoredMessage(sender, "&7Уникальных игроков: &d" + uniquePlayers);
        sendColoredMessage(sender, "&8Используйте &e/online stats &8для детальной статистики");
    }


    private void sendDetailedStats(CommandSender sender) {
        int currentOnline = getServer().getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();
        int totalSessions = database.getTotalSessions();
        int activeSessions = database.getActiveSessions();
        long totalPlaytime = database.getTotalPlaytime();
        long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        sendColoredMessage(sender, "&6&l=== &eДетальная статистика &6&l===");
        sendColoredMessage(sender, "&7Текущий онлайн: &a" + currentOnline);
        sendColoredMessage(sender, "&7Рекорд онлайна: &c&l" + maxOnline);
        sendColoredMessage(sender, "&7Уникальных игроков: &d" + uniquePlayers);
        sendColoredMessage(sender, "&7Всего сессий: &b" + totalSessions);
        sendColoredMessage(sender, "&7Среднее время игры: &e" + averageMinutes + " &7мин");
        sendColoredMessage(sender, "&7Активных сессий: &a" + activeSessions);

        if (currentOnline > 0) {
            String onlinePlayers = getServer().getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.joining("&7, &f"));
            sendColoredMessage(sender, "&7Онлайн: &f" + onlinePlayers);
        }
    }


    private void sendTopStats(CommandSender sender) {
        sendColoredMessage(sender, "&6&l=== &eТоп игроков по активности &6&l===");

        Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
        if (topPlayers.isEmpty()) {
            sendColoredMessage(sender, "&cПока нет данных о игроках");
            return;
        }

        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String medal = position == 1 ? "&6&l" : position == 2 ? "&7&l" : position == 3 ? "&c&l" : "&e";
            sendColoredMessage(sender, medal + position + ". &f" + entry.getKey() + "&7: &a" + entry.getValue() + " &7входов");
            position++;
        }
    }


    private void sendPlayerStats(CommandSender sender, String playerName) {
        Player player = getServer().getPlayer(playerName);
        int totalJoins = database.getPlayerJoinCount(playerName);
        long totalPlaytime = database.getPlayerTotalPlaytime(playerName);
        long totalHours = totalPlaytime / (1000 * 60 * 60);
        long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        sendColoredMessage(sender, "&6&l=== &eСтатистика игрока &b" + playerName + " &6&l===");

        if (player != null && player.isOnline()) {
            Long joinTime = playerJoinTimes.get(playerName);
            long sessionTime = joinTime != null ? System.currentTimeMillis() - joinTime : 0;
            long sessionMinutes = sessionTime / (1000 * 60);

            sendColoredMessage(sender, "&7Статус: &a&lОнлайн");
            sendColoredMessage(sender, "&7Текущая сессия: &e" + sessionMinutes + " &7минут");
            sendColoredMessage(sender, "&7Пинг: &b" + player.getPing() + " &7мс");
            sendColoredMessage(sender, "&7Локация: &d" + formatLocation(player.getLocation()));
        } else {
            sendColoredMessage(sender, "&7Статус: &8&lОффлайн");
        }

        if (totalJoins > 0) {
            sendColoredMessage(sender, "&7Всего входов: &a" + totalJoins);
            sendColoredMessage(sender, "&7Общее время игры: &e" + totalHours + " &7ч &e" + totalMinutes + " &7мин");
        } else {
            sendColoredMessage(sender, "&cИгрок не найден или никогда не заходил на сервер");
        }
    }


    private String formatLocation(Location loc) {
        return String.format("X:%.0f Y:%.0f Z:%.0f", loc.getX(), loc.getY(), loc.getZ());
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private void sendHourlyStats(CommandSender sender, int days) {
        Map<Integer, Double> hourlyAvg = database.getHourlyAverages(days);

        if (hourlyAvg.isEmpty()) {
            sendColoredMessage(sender, "&cНедостаточно данных для анализа. Подождите накопления статистики.");
            return;
        }

        sendColoredMessage(sender, "&6&l=== &eСредний онлайн по часам &7(за " + days + " дней) &6&l===");
        for (Map.Entry<Integer, Double> entry : hourlyAvg.entrySet()) {
            String hour = String.format("%02d:00", entry.getKey());
            String bar = createBar(entry.getValue(), getMaxValue(hourlyAvg.values()));
            sendColoredMessage(sender, "&b" + hour + " &7" + bar + " &a" + String.format("%.1f", entry.getValue()));
        }
    }

    private void sendDailyStats(CommandSender sender, int days) {
        Map<String, Double> dailyAvg = database.getDailyAverages(days);

        if (dailyAvg.isEmpty()) {
            sendColoredMessage(sender, "&cНедостаточно данных для анализа. Подождите накопления статистики.");
            return;
        }

        sendColoredMessage(sender, "&6&l=== &eСредний онлайн по дням &7(за " + days + " дней) &6&l===");
        for (Map.Entry<String, Double> entry : dailyAvg.entrySet()) {
            String bar = createBar(entry.getValue(), getMaxValue(dailyAvg.values()));
            sendColoredMessage(sender, "&d" + entry.getKey() + " &7" + bar + " &a" + String.format("%.1f", entry.getValue()));
        }
    }

    private void sendWeekdayStats(CommandSender sender, int weeks) {
        Map<String, Double> weekdayAvg = database.getWeekdayAverages(weeks);

        if (weekdayAvg.isEmpty()) {
            sendColoredMessage(sender, "&cНедостаточно данных для анализа. Подождите накопления статистики.");
            return;
        }

        sendColoredMessage(sender, "&6&l=== &eСредний онлайн по дням недели &7(за " + weeks + " недель) &6&l===");
        for (Map.Entry<String, Double> entry : weekdayAvg.entrySet()) {
            String bar = createBar(entry.getValue(), getMaxValue(weekdayAvg.values()));
            sendColoredMessage(sender, "&b" + String.format("%-12s", entry.getKey()) + " &7" + bar + " &a" + String.format("%.1f", entry.getValue()));
        }
    }

    private void sendPeakHours(CommandSender sender, int days) {
        Map<String, Integer> peakHours = database.getPeakHours(days);

        if (peakHours.isEmpty()) {
            sendColoredMessage(sender, "&cНедостаточно данных для анализа. Подождите накопления статистики.");
            return;
        }

        sendColoredMessage(sender, "&6&l=== &eПиковые часы активности &7(за " + days + " дней) &6&l===");
        int position = 1;
        for (Map.Entry<String, Integer> entry : peakHours.entrySet()) {
            String emoji = position == 1 ? "&c&l⚡" : position == 2 ? "&e⚡" : position == 3 ? "&a⚡" : "&7•";
            sendColoredMessage(sender, emoji + " &b" + entry.getKey() + " &7- пик &d" + entry.getValue() + " &7игроков");
            position++;
        }
    }

    private String createBar(double value, double maxValue) {
        int barLength = 20;
        int filledLength = (int) ((value / maxValue) * barLength);
        double percentage = (value / maxValue) * 100;

        // Цвет зависит от заполненности: красный->желтый->зеленый
        String barColor;
        if (percentage >= 75) {
            barColor = "&a"; // Зеленый - высокая активность
        } else if (percentage >= 50) {
            barColor = "&e"; // Желтый - средняя активность
        } else if (percentage >= 25) {
            barColor = "&6"; // Оранжевый - низкая активность
        } else {
            barColor = "&c"; // Красный - очень низкая активность
        }

        StringBuilder bar = new StringBuilder("&8[");
        bar.append(barColor);
        for (int i = 0; i < barLength; i++) {
            if (i < filledLength) {
                bar.append("█");
            } else {
                bar.append("&8░");
            }
        }
        bar.append("&8]");

        return bar.toString();
    }

    private double getMaxValue(java.util.Collection<Double> values) {
        return values.stream().max(Double::compare).orElse(1.0);
    }

    /**
     * Конвертирует цветовые коды в сообщении и отправляет игроку
     */
    private void sendColoredMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
    }
}