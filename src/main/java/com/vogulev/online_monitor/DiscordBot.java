package com.vogulev.online_monitor;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

import java.awt.Color;
import java.util.Map;
import java.util.logging.Logger;

public class DiscordBot extends ListenerAdapter {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private JDA jda;
    private final OnlineMonitorPlugin plugin;
    private String channelId;

    public DiscordBot(OnlineMonitorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(String token, String channelId) {
        this.channelId = channelId;

        try {
            logger.info("Создание JDA соединения...");
            logger.info("Токен начинается с: " + (token.length() > 10 ? token.substring(0, 10) + "..." : "слишком короткий"));
            logger.info("Channel ID: " + channelId);

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .setActivity(Activity.watching("Minecraft сервер"))
                    .addEventListeners(this)
                    .build();

            logger.info("JDA создан, ожидание готовности...");
            jda.awaitReady();
            logger.info("JDA готов!");

            logger.info("Регистрация slash команд...");
            jda.updateCommands().addCommands(
                    Commands.slash("online", "Показать текущий онлайн на сервере"),
                    Commands.slash("stats", "Показать детальную статистику сервера"),
                    Commands.slash("top", "Показать топ игроков по активности"),
                    Commands.slash("player", "Показать статистику игрока")
                            .addOption(OptionType.STRING, "nickname", "Никнейм игрока", true)
            ).queue(
                success -> logger.info("Slash команды успешно зарегистрированы!"),
                error -> logger.warning("Ошибка регистрации команд: " + error.getMessage())
            );

            logger.info("Discord bot успешно запущен! Статус: " + jda.getStatus());
        } catch (InterruptedException e) {
            logger.severe("Ошибка ожидания запуска Discord бота: " + e.getMessage());
            e.printStackTrace();
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.severe("Ошибка запуска Discord бота: " + e.getClass().getName() + ": " + e.getMessage());
            logger.severe("Возможные причины:");
            logger.severe("  1. Неправильный токен бота");
            logger.severe("  2. Не включен MESSAGE CONTENT INTENT в Discord Developer Portal");
            logger.severe("  3. Бот был удален или токен устарел");
            logger.severe("  4. Проблемы с подключением к Discord API");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        if (jda != null) {
            jda.shutdown();
            logger.info("Discord bot остановлен");
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        switch (event.getName()) {
            case "online":
                handleOnlineCommand(event);
                break;
            case "stats":
                handleStatsCommand(event);
                break;
            case "top":
                handleTopCommand(event);
                break;
            case "player":
                handlePlayerCommand(event);
                break;
        }
    }

    private void handleOnlineCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        int currentOnline = plugin.getServer().getOnlinePlayers().size();
        int maxOnline = plugin.getDatabase().getMaxOnline();
        int uniquePlayers = plugin.getDatabase().getUniquePlayersCount();

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Статистика онлайна")
                .setColor(Color.GREEN)
                .addField("🟢 Сейчас онлайн", currentOnline + " игроков", true)
                .addField("🏆 Максимум онлайна", String.valueOf(maxOnline), true)
                .addField("👥 Уникальных игроков", String.valueOf(uniquePlayers), true)
                .setFooter("OnlineMonitor", null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleStatsCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        DatabaseManager db = plugin.getDatabase();
        int currentOnline = plugin.getServer().getOnlinePlayers().size();
        int maxOnline = db.getMaxOnline();
        int uniquePlayers = db.getUniquePlayersCount();
        int totalSessions = db.getTotalSessions();
        int activeSessions = db.getActiveSessions();
        long totalPlaytime = db.getTotalPlaytime();
        long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📈 Детальная статистика")
                .setColor(Color.BLUE)
                .addField("🟢 Текущий онлайн", String.valueOf(currentOnline), true)
                .addField("🏆 Рекорд онлайна", String.valueOf(maxOnline), true)
                .addField("👥 Уникальных игроков", String.valueOf(uniquePlayers), true)
                .addField("📝 Всего сессий", String.valueOf(totalSessions), true)
                .addField("⏱️ Среднее время игры", averageMinutes + " мин", true)
                .addField("🎮 Активных сессий", String.valueOf(activeSessions), true)
                .setFooter("OnlineMonitor", null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleTopCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        Map<String, Integer> topPlayers = plugin.getDatabase().getTopPlayersByJoins(10);

        if (topPlayers.isEmpty()) {
            event.getHook().sendMessage("❌ Пока нет данных о игроках").queue();
            return;
        }

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏅 Топ игроков по активности")
                .setColor(Color.ORANGE);

        int position = 1;
        StringBuilder topList = new StringBuilder();
        for (Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            String medal = position == 1 ? "🥇" : position == 2 ? "🥈" : position == 3 ? "🥉" : "▪️";
            topList.append(medal).append(" **").append(position).append(".** ")
                    .append(entry.getKey()).append(" - ")
                    .append(entry.getValue()).append(" входов\n");
            position++;
        }

        embed.setDescription(topList.toString());
        embed.setFooter("OnlineMonitor", null);
        embed.setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handlePlayerCommand(SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        String playerName = event.getOption("nickname").getAsString();
        DatabaseManager db = plugin.getDatabase();

        int totalJoins = db.getPlayerJoinCount(playerName);
        long totalPlaytime = db.getPlayerTotalPlaytime(playerName);
        long totalHours = totalPlaytime / (1000 * 60 * 60);
        long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        if (totalJoins == 0) {
            event.getHook().sendMessage("❌ Игрок **" + playerName + "** не найден или никогда не заходил на сервер").queue();
            return;
        }

        boolean isOnline = plugin.getServer().getPlayer(playerName) != null;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("👤 Статистика игрока " + playerName)
                .setColor(isOnline ? Color.GREEN : Color.GRAY)
                .addField("📊 Статус", isOnline ? "🟢 Онлайн" : "⚫ Оффлайн", true)
                .addField("🔢 Всего входов", String.valueOf(totalJoins), true)
                .addField("⏱️ Общее время игры", totalHours + " ч " + totalMinutes + " мин", true)
                .setFooter("OnlineMonitor", null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    // === Методы для отправки уведомлений ===

    public void sendPlayerJoinNotification(String playerName, int currentOnline, boolean isNewPlayer) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setDescription((isNewPlayer ? "🎉 **Новый игрок** " : "🎮 ") +
                        "**" + playerName + "** зашел на сервер")
                .addField("Онлайн", currentOnline + " игроков", false)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendPlayerQuitNotification(String playerName, int currentOnline, long sessionMinutes) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setDescription("👋 **" + playerName + "** вышел с сервера")
                .addField("Онлайн", currentOnline + " игроков", false)
                .addField("Время в игре", sessionMinutes + " мин", false)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendNewRecordNotification(int newRecord) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🏆 НОВЫЙ РЕКОРД ОНЛАЙНА!")
                .setColor(Color.RED)
                .setDescription("Достигнут новый рекорд: **" + newRecord + " игроков!** 🎉")
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendServerStartNotification() {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🟢 Сервер запущен")
                .setColor(Color.GREEN)
                .setDescription("Minecraft сервер успешно запущен и готов к игре!")
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }

    public void sendServerStopNotification() {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🔴 Сервер остановлен")
                .setColor(Color.RED)
                .setDescription("Minecraft сервер был остановлен")
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue();
    }
}
