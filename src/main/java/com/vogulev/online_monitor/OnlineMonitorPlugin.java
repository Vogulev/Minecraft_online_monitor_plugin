package com.vogulev.online_monitor;

import com.vogulev.online_monitor.commands.StatsCommandExecutor;
import com.vogulev.online_monitor.listeners.PlayerEventListener;
import com.vogulev.online_monitor.tasks.CleanupTask;
import com.vogulev.online_monitor.tasks.SnapshotTask;
import com.vogulev.online_monitor.tasks.UpdateScoreboardTask;
import com.vogulev.online_monitor.ui.ScoreboardServerStatisticsManager;
import com.vogulev.online_monitor.web.WebServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static com.vogulev.online_monitor.i18n.LocalizationManager.initialize;

/**
 * Главный класс плагина OnlineMonitor
 * Отвечает только за lifecycle и координацию компонентов
 */
public class OnlineMonitorPlugin extends JavaPlugin {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private DatabaseManager database;
    private DiscordBot discordBot;
    private WebServer webServer;
    private ScoreboardServerStatisticsManager scoreboardServerStatisticsManager;
    private final Map<String, Long> playerJoinTimes = new HashMap<>();
    private int lastMaxOnline = 0;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Initialize localization
        String language = getConfig().getString("language", "en");
        initialize(language);

        database = new DatabaseManager(getDataFolder());
        database.setPlugin(this);

        try {
            initializeDatabase();
        } catch (Exception e) {
            logger.severe("Failed to initialize database: " + e.getMessage());
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        lastMaxOnline = database.getMaxOnline();

        boolean scoreboardEnabled = getConfig().getBoolean("scoreboard.enabled", true);
        scoreboardServerStatisticsManager = new ScoreboardServerStatisticsManager(database, scoreboardEnabled);
        logger.info("Scoreboard UI enabled = " + scoreboardEnabled);

        PlayerEventListener playerListener = new PlayerEventListener(
                database,
                discordBot,
                getServer(),
                getConfig(),
                playerJoinTimes,
                this::checkNewRecord
        );
        playerListener.setScoreboardManager(scoreboardServerStatisticsManager);
        getServer().getPluginManager().registerEvents(playerListener, this);

        StatsCommandExecutor statsCommand = new StatsCommandExecutor(database, getServer(), playerJoinTimes,
                scoreboardServerStatisticsManager);
        getCommand("online").setExecutor(statsCommand);
        getCommand("online").setTabCompleter(statsCommand);

        scheduleTasks();

        initializeDiscord();

        initializeWebServer();

        logger.info("OnlineMonitor plugin enabled with database!");
    }

    @Override
    public void onDisable() {
        if (discordBot != null && getConfig().getBoolean("discord.notifications.server-stop", true)) {
            discordBot.sendServerStopNotification();
        }
        for (Map.Entry<String, Long> entry : playerJoinTimes.entrySet()) {
            long sessionDuration = System.currentTimeMillis() - entry.getValue();
            database.recordPlayerQuit(entry.getKey(), sessionDuration);
        }
        playerJoinTimes.clear();
        if (webServer != null) {
            logger.info("Stopping web server...");
            webServer.stop();
        }
        if (discordBot != null) {
            logger.info("Shutting down Discord bot...");
            discordBot.shutdown();
        }
        if (database != null) {
            logger.info("Closing database connection...");
            database.disconnect();
        }
        logger.info("OnlineMonitor plugin disabled!");
    }

    private void initializeDatabase() throws Exception {
        String timezoneOffset = getConfig().getString("timezone-offset", "+3");
        database.setTimezoneOffset(timezoneOffset);

        String dbType = getConfig().getString("database.type", "sqlite");
        String host = getConfig().getString("database.mysql.host", "localhost");
        int port = getConfig().getInt("database.mysql.port", 3306);
        String dbName = getConfig().getString("database.mysql.database", "minecraft_stats");
        String username = getConfig().getString("database.mysql.username", "root");
        String password = getConfig().getString("database.mysql.password", "password");

        database.connect(dbType, host, port, dbName, username, password);
    }

    private void scheduleTasks() {
        long snapshotInterval = getConfig().getLong("snapshot-interval-minutes", 5) * 60 * 20; // В тиках
        getServer().getScheduler().runTaskTimer(
                this,
                new SnapshotTask(database, getServer()),
                snapshotInterval,
                snapshotInterval
        );

        int daysToKeep = getConfig().getInt("snapshot-days-to-keep", 30);
        getServer().getScheduler().runTaskTimer(
                this,
                new CleanupTask(database, daysToKeep),
                24000L,
                24000L
        );

        long scoreboardUpdateInterval = getConfig().getLong("scoreboard.update-interval-seconds", 1) * 20L;
        getServer().getScheduler().runTaskTimer(
                this,
                new UpdateScoreboardTask(scoreboardServerStatisticsManager),
                20L,
                scoreboardUpdateInterval
        );

        logger.info("Online snapshots will be recorded every " + (snapshotInterval / 1200) + " minutes");
        logger.info("Scoreboard will be updated every " + (scoreboardUpdateInterval / 20) + " seconds");
    }

    private void initializeDiscord() {
        boolean discordEnabled = getConfig().getBoolean("discord.enabled", false);
        logger.info("Discord integration enabled=" + discordEnabled);

        if (!discordEnabled) {
            logger.info("Discord bot disabled in configuration (discord.enabled=false)");
            return;
        }

        String botToken = getConfig().getString("discord.bot-token");
        String channelId = getConfig().getString("discord.channel-id");

        logger.info("Discord bot-token length: " + (botToken != null ? botToken.length() : "null"));
        logger.info("Discord channel-id: " + (channelId != null ? channelId : "null"));

        if (botToken != null && !botToken.equals("YOUR_BOT_TOKEN_HERE") &&
            channelId != null && !channelId.equals("YOUR_CHANNEL_ID_HERE")) {

            logger.info("Starting Discord bot...");
            discordBot = new DiscordBot(this);
            discordBot.start(botToken, channelId);

            if (getConfig().getBoolean("discord.notifications.server-start", true)) {
                getServer().getScheduler().runTaskLater(this,
                        () -> discordBot.sendServerStartNotification(), 40L);
            }
        } else {
            logger.warning("Discord bot not started: check bot-token and channel-id settings in config.yml");
            if (botToken == null || botToken.equals("YOUR_BOT_TOKEN_HERE")) {
                logger.warning("  - bot-token is not configured!");
            }
            if (channelId == null || channelId.equals("YOUR_CHANNEL_ID_HERE")) {
                logger.warning("  - channel-id is not configured!");
            }
        }
    }

    private void initializeWebServer() {
        boolean webEnabled = getConfig().getBoolean("web-panel.enabled", false);
        logger.info("Web panel enabled = " + webEnabled);

        if (!webEnabled) {
            logger.info("Web panel disabled in configuration (web-panel.enabled = false)");
            return;
        }

        int port = getConfig().getInt("web-panel.port", 8080);

        try {
            webServer = new WebServer(this, database, port);
            webServer.start();
        } catch (Exception e) {
            logger.severe("Failed to start web panel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void checkNewRecord() {
        int currentMaxOnline = database.getMaxOnline();
        if (currentMaxOnline > lastMaxOnline) {
            lastMaxOnline = currentMaxOnline;

            if (discordBot != null && getConfig().getBoolean("discord.notifications.new-record", true)) {
                discordBot.sendNewRecordNotification(currentMaxOnline);
            }

            logger.info("New online record: " + currentMaxOnline + " players!");
        }
    }

    public DatabaseManager getDatabase() {
        return database;
    }
}