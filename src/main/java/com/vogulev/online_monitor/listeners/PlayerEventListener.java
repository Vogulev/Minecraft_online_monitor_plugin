package com.vogulev.online_monitor.listeners;

import com.vogulev.online_monitor.DatabaseManager;
import com.vogulev.online_monitor.DiscordBot;
import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;
import org.bukkit.ChatColor;
import org.bukkit.Server;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.logging.Logger;

import static com.vogulev.online_monitor.i18n.LocalizationManager.getMessage;

/**
 * Handler for player join and quit events
 */
public class PlayerEventListener implements Listener {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final DatabaseManager database;
    private final DiscordBot discordBot;
    private final Server server;
    private final FileConfiguration config;
    private final Map<String, Long> playerJoinTimes;
    private final Runnable onNewRecordCallback;
    private ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;

    public PlayerEventListener(final DatabaseManager database, final DiscordBot discordBot, final Server server,
                                final FileConfiguration config, final Map<String, Long> playerJoinTimes,
                                final Runnable onNewRecordCallback) {
        this.database = database;
        this.discordBot = discordBot;
        this.server = server;
        this.config = config;
        this.playerJoinTimes = playerJoinTimes;
        this.onNewRecordCallback = onNewRecordCallback;
    }

    public void setScoreboardManager(final ScoreboardServerStatisticsManager scoreboardServerStatisticsManager) {
        this.scoreboardServerStatisticsManager = scoreboardServerStatisticsManager;
    }

    @EventHandler
    public void onPlayerJoin(final PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        updateMaxOnline();

        final boolean isFirstTime = !player.hasPlayedBefore();
        if (isFirstTime) {
            database.incrementUniquePlayer();
            logger.info("New player joined: " + player.getName());
        }

        database.recordPlayerJoin(playerName);

        playerJoinTimes.put(playerName, System.currentTimeMillis());

        String welcomeMessage = config.getString("welcome-message",
                getMessage("welcome.default"));

        welcomeMessage = welcomeMessage
                .replace("%player%", playerName)
                .replace("%online%", String.valueOf(server.getOnlinePlayers().size()));

        welcomeMessage = ChatColor.translateAlternateColorCodes('&', welcomeMessage);

        player.sendMessage(welcomeMessage);

        if (discordBot != null) {
            final boolean notifyJoin = config.getBoolean("discord.notifications.player-join", true);
            final boolean notifyNewPlayer = config.getBoolean("discord.notifications.new-player", true);

            if ((notifyJoin && !isFirstTime) || (notifyNewPlayer && isFirstTime)) {
                final int currentOnline = server.getOnlinePlayers().size();
                discordBot.sendPlayerJoinNotification(playerName, currentOnline, isFirstTime);
            }
        }

        if (onNewRecordCallback != null) {
            onNewRecordCallback.run();
        }

        if (scoreboardServerStatisticsManager != null) {
            scoreboardServerStatisticsManager.showScoreboard(player);
            scoreboardServerStatisticsManager.updateScoreboard(player);
        }

        logger.info(player.getName() + " joined. Online: " + server.getOnlinePlayers().size());
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        final String playerName = player.getName();

        final Long joinTime = playerJoinTimes.get(playerName);
        if (joinTime != null) {
            final long sessionTime = System.currentTimeMillis() - joinTime;
            final long minutes = sessionTime / (1000 * 60);

            database.recordPlayerQuit(playerName, sessionTime);

            logger.info(playerName + " spent in game: " + minutes + " minutes");

            if (discordBot != null && config.getBoolean("discord.notifications.player-quit", true)) {
                final int currentOnline = server.getOnlinePlayers().size() - 1;
                discordBot.sendPlayerQuitNotification(playerName, currentOnline, minutes);
            }

            playerJoinTimes.remove(playerName);
        }

        if (scoreboardServerStatisticsManager != null) {
            scoreboardServerStatisticsManager.removePlayer(player);
        }

        logger.info(player.getName() + " left. Online: " + (server.getOnlinePlayers().size() - 1));
    }

    private void updateMaxOnline() {
        final int currentOnline = server.getOnlinePlayers().size();
        database.updateMaxOnline(currentOnline);
    }
}
