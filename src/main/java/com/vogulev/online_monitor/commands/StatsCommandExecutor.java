package com.vogulev.online_monitor.commands;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.formatters.StatsFormatter;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Обработчик команд /online
 */
public class StatsCommandExecutor implements CommandExecutor, TabCompleter {
    private final DatabaseManager database;
    private final Server server;
    private final Map<String, Long> playerJoinTimes;

    public StatsCommandExecutor(DatabaseManager database, Server server, Map<String, Long> playerJoinTimes) {
        this.database = database;
        this.server = server;
        this.playerJoinTimes = playerJoinTimes;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
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
                    StatsFormatter.sendColoredMessage(sender, "&cИспользование: &e/online player <ник>");
                }
                break;
            case "hourly":
                int days = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendHourlyStats(sender, database.getHourlyAverages(days), days);
                break;
            case "daily":
                int daysDaily = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendDailyStats(sender, database.getDailyAverages(daysDaily), daysDaily);
                break;
            case "weekday":
                int weeks = args.length > 1 ? parseIntOrDefault(args[1], 4) : 4;
                StatsFormatter.sendWeekdayStats(sender, database.getWeekdayAverages(weeks), weeks);
                break;
            case "peak":
                int daysPeak = args.length > 1 ? parseIntOrDefault(args[1], 7) : 7;
                StatsFormatter.sendPeakHours(sender, database.getPeakHours(daysPeak), daysPeak);
                break;
            default:
                StatsFormatter.sendColoredMessage(sender, "&cНеизвестная команда.");
                StatsFormatter.sendColoredMessage(sender, "&7Используйте: &e/online [stats|top|player|hourly|daily|weekday|peak]");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Stream.of("stats", "top", "player", "hourly", "daily", "weekday", "peak", "help")
                    .filter(option -> option.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("player")) {
            return server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }

        return Collections.emptyList();
    }

    private void sendBasicStats(CommandSender sender) {
        int currentOnline = server.getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();

        StatsFormatter.sendColoredMessage(sender, "&6&l=== &eСтатистика онлайна &6&l===");
        StatsFormatter.sendColoredMessage(sender, "&7Сейчас онлайн: &a" + currentOnline + " &7игроков");
        StatsFormatter.sendColoredMessage(sender, "&7Максимум онлайна: &b" + maxOnline);
        StatsFormatter.sendColoredMessage(sender, "&7Уникальных игроков: &d" + uniquePlayers);
        StatsFormatter.sendColoredMessage(sender, "&8Используйте &e/online stats &8для детальной статистики");
    }

    private void sendDetailedStats(CommandSender sender) {
        int currentOnline = server.getOnlinePlayers().size();
        int maxOnline = database.getMaxOnline();
        int uniquePlayers = database.getUniquePlayersCount();
        int totalSessions = database.getTotalSessions();
        int activeSessions = database.getActiveSessions();
        long totalPlaytime = database.getTotalPlaytime();
        long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        StatsFormatter.sendColoredMessage(sender, "&6&l=== &eДетальная статистика &6&l===");
        StatsFormatter.sendColoredMessage(sender, "&7Текущий онлайн: &a" + currentOnline);
        StatsFormatter.sendColoredMessage(sender, "&7Рекорд онлайна: &c&l" + maxOnline);
        StatsFormatter.sendColoredMessage(sender, "&7Уникальных игроков: &d" + uniquePlayers);
        StatsFormatter.sendColoredMessage(sender, "&7Всего сессий: &b" + totalSessions);
        StatsFormatter.sendColoredMessage(sender, "&7Среднее время игры: &e" + averageMinutes + " &7мин");
        StatsFormatter.sendColoredMessage(sender, "&7Активных сессий: &a" + activeSessions);

        if (currentOnline > 0) {
            String onlinePlayers = server.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.joining("&7, &f"));
            StatsFormatter.sendColoredMessage(sender, "&7Онлайн: &f" + onlinePlayers);
        }
    }

    private void sendTopStats(CommandSender sender) {
        StatsFormatter.sendColoredMessage(sender, "&6&l=== &eТоп игроков по активности &6&l===");

        Map<String, Integer> topPlayers = database.getTopPlayersByJoins(10);
        if (topPlayers.isEmpty()) {
            StatsFormatter.sendColoredMessage(sender, "&cПока нет данных о игроках");
            return;
        }

        int position = 1;
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String medal = position == 1 ? "&6&l" : position == 2 ? "&7&l" : position == 3 ? "&c&l" : "&e";
            StatsFormatter.sendColoredMessage(sender, medal + position + ". &f" + entry.getKey() + "&7: &a" + entry.getValue() + " &7входов");
            position++;
        }
    }

    private void sendPlayerStats(CommandSender sender, String playerName) {
        Player player = server.getPlayer(playerName);
        int totalJoins = database.getPlayerJoinCount(playerName);
        long totalPlaytime = database.getPlayerTotalPlaytime(playerName);
        long totalHours = totalPlaytime / (1000 * 60 * 60);
        long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        StatsFormatter.sendColoredMessage(sender, "&6&l=== &eСтатистика игрока &b" + playerName + " &6&l===");

        if (player != null && player.isOnline()) {
            Long joinTime = playerJoinTimes.get(playerName);
            long sessionTime = joinTime != null ? System.currentTimeMillis() - joinTime : 0;
            long sessionMinutes = sessionTime / (1000 * 60);

            StatsFormatter.sendColoredMessage(sender, "&7Статус: &a&lОнлайн");
            StatsFormatter.sendColoredMessage(sender, "&7Текущая сессия: &e" + sessionMinutes + " &7минут");
            StatsFormatter.sendColoredMessage(sender, "&7Пинг: &b" + player.getPing() + " &7мс");
            StatsFormatter.sendColoredMessage(sender, "&7Локация: &d" + formatLocation(player.getLocation()));
        } else {
            StatsFormatter.sendColoredMessage(sender, "&7Статус: &8&lОффлайн");
        }

        if (totalJoins > 0) {
            StatsFormatter.sendColoredMessage(sender, "&7Всего входов: &a" + totalJoins);
            StatsFormatter.sendColoredMessage(sender, "&7Общее время игры: &e" + totalHours + " &7ч &e" + totalMinutes + " &7мин");
        } else {
            StatsFormatter.sendColoredMessage(sender, "&cИгрок не найден или никогда не заходил на сервер");
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
}
