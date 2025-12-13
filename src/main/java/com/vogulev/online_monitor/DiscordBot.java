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

import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;

public class DiscordBot extends ListenerAdapter {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private JDA jda;
    private final OnlineMonitorPlugin plugin;
    private String channelId;

    public DiscordBot(final OnlineMonitorPlugin plugin) {
        this.plugin = plugin;
    }

    public void start(final String token, final String channelId) {
        this.channelId = channelId;

        try {
            logger.info("Creating JDA connection...");
            logger.info("Token starts with: " + (token.length() > 10 ? token.substring(0, 10) + "..." : "too short"));
            logger.info("Channel ID: " + channelId);

            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .setActivity(Activity.watching(getMessage("discord.activity.watching")))
                    .addEventListeners(this)
                    .build();

            logger.info("JDA created, waiting for ready...");
            jda.awaitReady();
            logger.info("JDA ready!");

            logger.info("Registering slash commands...");
            jda.updateCommands().addCommands(
                    Commands.slash("online", getMessage("discord.command.online")),
                    Commands.slash("stats", getMessage("discord.command.stats")),
                    Commands.slash("top", getMessage("discord.command.top")),
                    Commands.slash("player", getMessage("discord.command.player"))
                            .addOption(OptionType.STRING, "nickname", getMessage("discord.command.player.option"), true)
            ).queue(
                success -> logger.info("Slash commands successfully registered!"),
                error -> logger.warning("Error registering commands: " + error.getMessage())
            );

            logger.info("Discord bot successfully started! Status: " + jda.getStatus());
        } catch (final InterruptedException e) {
            logger.severe("Error waiting for Discord bot to start: " + e.getMessage());
            Thread.currentThread().interrupt();
        } catch (final Exception e) {
            logger.severe("Error starting Discord bot: " + e.getClass().getName() + ": " + e.getMessage());
            logger.severe("Possible reasons:");
            logger.severe("  1. Incorrect bot token");
            logger.severe("  2. MESSAGE CONTENT INTENT not enabled in Discord Developer Portal");
            logger.severe("  3. Bot was deleted or token is expired");
            logger.severe("  4. Problems connecting to Discord API");
        }
    }

    public void shutdown() {
        if (jda != null) {
            try
            {
                jda.shutdown();
                if (!jda.awaitShutdown(5, java.util.concurrent.TimeUnit.SECONDS))
                {
                    jda.shutdownNow();
                    jda.awaitShutdown();
                }
                logger.info("Discord bot stopped");
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                jda.shutdownNow();
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(final SlashCommandInteractionEvent event) {
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

    private void handleOnlineCommand(final SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final int currentOnline = plugin.getServer().getOnlinePlayers().size();
        final int maxOnline = plugin.getDatabase().getMaxOnline();
        final int uniquePlayers = plugin.getDatabase().getUniquePlayersCount();

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.embed.online.title"))
                .setColor(Color.GREEN)
                .addField(getMessage("discord.embed.online.current"),
                          getMessage("discord.embed.online.current.value", currentOnline), true)
                .addField(getMessage("discord.embed.online.max"), String.valueOf(maxOnline), true)
                .addField(getMessage("discord.embed.online.unique"), String.valueOf(uniquePlayers), true)
                .setFooter(getMessage("discord.embed.footer"), null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleStatsCommand(final SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final DatabaseManager db = plugin.getDatabase();
        final int currentOnline = plugin.getServer().getOnlinePlayers().size();
        final int maxOnline = db.getMaxOnline();
        final int uniquePlayers = db.getUniquePlayersCount();
        final int totalSessions = db.getTotalSessions();
        final int activeSessions = db.getActiveSessions();
        final long totalPlaytime = db.getTotalPlaytime();
        final long averageMinutes = uniquePlayers > 0 ? (totalPlaytime / uniquePlayers) / (1000 * 60) : 0;

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.embed.stats.title"))
                .setColor(Color.BLUE)
                .addField(getMessage("discord.embed.stats.current"), String.valueOf(currentOnline), true)
                .addField(getMessage("discord.embed.stats.record"), String.valueOf(maxOnline), true)
                .addField(getMessage("discord.embed.stats.unique"), String.valueOf(uniquePlayers), true)
                .addField(getMessage("discord.embed.stats.sessions"), String.valueOf(totalSessions), true)
                .addField(getMessage("discord.embed.stats.avg_time"),
                          getMessage("discord.embed.stats.avg_time.value", averageMinutes), true)
                .addField(getMessage("discord.embed.stats.active"), String.valueOf(activeSessions), true)
                .setFooter(getMessage("discord.embed.footer"), null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handleTopCommand(final SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final Map<String, Integer> topPlayers = plugin.getDatabase().getTopPlayersByJoins(10);

        if (topPlayers.isEmpty()) {
            event.getHook().sendMessage(getMessage("discord.embed.top.empty")).queue();
            return;
        }

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.embed.top.title"))
                .setColor(Color.ORANGE);

        int position = 1;
        final StringBuilder topList = new StringBuilder();
        for (final Map.Entry<String, Integer> entry : topPlayers.entrySet()) {
            final String medal = position == 1 ? "🥇" : position == 2 ? "🥈" : position == 3 ? "🥉" : "▪️";
            topList.append(medal).append(" **").append(position).append(".** ")
                    .append(entry.getKey()).append(" - ")
                    .append(getMessage("discord.embed.top.joins", entry.getValue())).append("\n");
            position++;
        }

        embed.setDescription(topList.toString());
        embed.setFooter(getMessage("discord.embed.footer"), null);
        embed.setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }

    private void handlePlayerCommand(final SlashCommandInteractionEvent event) {
        event.deferReply().queue();

        final String playerName = event.getOption("nickname").getAsString();
        final DatabaseManager db = plugin.getDatabase();

        final int totalJoins = db.getPlayerJoinCount(playerName);
        final long totalPlaytime = db.getPlayerTotalPlaytime(playerName);
        final long totalHours = totalPlaytime / (1000 * 60 * 60);
        final long totalMinutes = (totalPlaytime / (1000 * 60)) % 60;

        if (totalJoins == 0) {
            event.getHook().sendMessage(getMessage("discord.embed.player.not_found", playerName)).queue();
            return;
        }

        final boolean isOnline = plugin.getServer().getPlayer(playerName) != null;

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.embed.player.title", playerName))
                .setColor(isOnline ? Color.GREEN : Color.GRAY)
                .addField(getMessage("discord.embed.player.status"),
                          isOnline ? getMessage("discord.embed.player.online") : getMessage("discord.embed.player.offline"), true)
                .addField(getMessage("discord.embed.player.joins"), String.valueOf(totalJoins), true)
                .addField(getMessage("discord.embed.player.playtime"),
                          getMessage("discord.embed.player.playtime.value", totalHours, totalMinutes), true)
                .setFooter(getMessage("discord.embed.footer"), null)
                .setTimestamp(java.time.Instant.now());

        event.getHook().sendMessageEmbeds(embed.build()).queue();
    }
    // === Methods for sending notifications ===

    public void sendPlayerJoinNotification(final String playerName, final int currentOnline, final boolean isNewPlayer) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        final TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        final String message = isNewPlayer ?
                getMessage("discord.notification.join.new", playerName) :
                getMessage("discord.notification.join", playerName);

        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GREEN)
                .setDescription(message)
                .addField(getMessage("discord.notification.online"),
                          getMessage("discord.notification.online.value", currentOnline), false)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
            success -> {},
            error -> logger.warning("Failed to send join notification: " + error.getMessage())
        );
    }

    public void sendPlayerQuitNotification(final String playerName, final int currentOnline, final long sessionMinutes) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        final TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        final EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.ORANGE)
                .setDescription(getMessage("discord.notification.quit", playerName))
                .addField(getMessage("discord.notification.online"),
                          getMessage("discord.notification.online.value", currentOnline), false)
                .addField(getMessage("discord.notification.playtime"),
                          getMessage("discord.notification.playtime.value", sessionMinutes), false)
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
            success -> {},
            error -> logger.warning("Failed to send quit notification: " + error.getMessage())
        );
    }

    public void sendNewRecordNotification(final int newRecord) {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        final TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.notification.record.title"))
                .setColor(Color.RED)
                .setDescription(getMessage("discord.notification.record.message", newRecord))
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
            success -> {},
            error -> logger.warning("Failed to send record notification: " + error.getMessage())
        );
    }

    public void sendServerStartNotification() {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        final TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.notification.server.start.title"))
                .setColor(Color.GREEN)
                .setDescription(getMessage("discord.notification.server.start.message"))
                .setTimestamp(java.time.Instant.now());

        channel.sendMessageEmbeds(embed.build()).queue(
            success -> {},
            error -> logger.warning("Failed to send server start notification: " + error.getMessage())
        );
    }

    public void sendServerStopNotification() {
        if (jda == null || channelId == null || channelId.isEmpty()) return;

        final TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;

        final EmbedBuilder embed = new EmbedBuilder()
                .setTitle(getMessage("discord.notification.server.stop.title"))
                .setColor(Color.RED)
                .setDescription(getMessage("discord.notification.server.stop.message"))
                .setTimestamp(java.time.Instant.now());

        try {
            channel.sendMessageEmbeds(embed.build()).complete();
        } catch (final Exception e) {
            logger.warning("Can't send notification about server stopped: " + e.getMessage());
        }

    }
}
