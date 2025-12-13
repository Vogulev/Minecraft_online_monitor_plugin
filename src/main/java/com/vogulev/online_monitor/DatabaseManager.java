package com.vogulev.online_monitor;

import com.vogulev.online_monitor.database.ConnectionManager;
import com.vogulev.online_monitor.database.repositories.AnalyticsRepository;
import com.vogulev.online_monitor.database.repositories.PlayerStatsRepository;
import com.vogulev.online_monitor.database.repositories.ServerStatsRepository;
import com.vogulev.online_monitor.database.repositories.SessionRepository;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * Facade for database operations.
 * Delegates calls to appropriate repositories.
 */
public class DatabaseManager {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final File dataFolder;

    private ConnectionManager connectionManager;
    private ServerStatsRepository serverStatsRepo;
    private PlayerStatsRepository playerStatsRepo;
    private SessionRepository sessionRepo;
    private AnalyticsRepository analyticsRepo;

    public DatabaseManager(final File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void setTimezoneOffset(final String offset) {
        if (connectionManager != null) {
            connectionManager.setTimezoneOffset(offset);
        }
    }

    public void connect(final org.bukkit.configuration.file.FileConfiguration config) throws SQLException {
        connectionManager = new ConnectionManager();
        connectionManager.connect(config, dataFolder);

        serverStatsRepo = new ServerStatsRepository(connectionManager);
        playerStatsRepo = new PlayerStatsRepository(connectionManager);
        sessionRepo = new SessionRepository(connectionManager);
        analyticsRepo = new AnalyticsRepository(connectionManager);
    }

    public void disconnect() {
        if (connectionManager != null) {
            connectionManager.disconnect();
        }
    }

    // === Async Helper Methods ===
    private CompletableFuture<Void> runAsync(final Runnable task) {
        return CompletableFuture.runAsync(() -> {
            try {
                task.run();
            } catch (Exception e) {
                logger.severe("Error in async database operation: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    // === Server Stats Methods (delegate to ServerStatsRepository) ===
    public void updateMaxOnline(final int currentOnline) {
        runAsync(() -> serverStatsRepo.updateMaxOnline(currentOnline));
    }

    public int getMaxOnline() {
        return serverStatsRepo.getMaxOnline();
    }

    public void incrementUniquePlayer() {
        runAsync(() -> serverStatsRepo.incrementUniquePlayer());
    }

    public int getUniquePlayersCount() {
        return serverStatsRepo.getUniquePlayersCount();
    }

    // === Player Stats Methods (delegate to PlayerStatsRepository) ===
    public void recordPlayerJoin(final String playerName) {
        runAsync(() -> {
            playerStatsRepo.recordPlayerJoin(playerName);
            sessionRepo.createSession(playerName);
        });
    }

    public void recordPlayerQuit(final String playerName, final long sessionDuration) {
        runAsync(() -> {
            sessionRepo.closeSession(playerName, sessionDuration);
            playerStatsRepo.updatePlaytime(playerName, sessionDuration);
        });
    }

    public int getPlayerJoinCount(final String playerName) {
        return playerStatsRepo.getPlayerJoinCount(playerName);
    }

    public long getPlayerTotalPlaytime(final String playerName) {
        return playerStatsRepo.getPlayerTotalPlaytime(playerName);
    }

    public Map<String, Integer> getTopPlayersByJoins(final int limit) {
        return playerStatsRepo.getTopPlayersByJoins(limit);
    }

    public long getTotalPlaytime() {
        return playerStatsRepo.getTotalPlaytime();
    }

    // === Session Methods (delegate to SessionRepository) ===

    public int getTotalSessions() {
        return sessionRepo.getTotalSessions();
    }

    public int getActiveSessions() {
        return sessionRepo.getActiveSessions();
    }

    // === Analytics Methods (delegate to AnalyticsRepository) ===

    public void recordOnlineSnapshot(final int onlineCount) {
        runAsync(() -> analyticsRepo.recordOnlineSnapshot(onlineCount));
    }

    public Map<Integer, Double> getHourlyAverages(final int days) {
        return analyticsRepo.getHourlyAverages(days);
    }

    public Map<String, Double> getDailyAverages(final int days) {
        return analyticsRepo.getDailyAverages(days);
    }

    public Map<Integer, Double> getWeekdayAverages(final int weeks) {
        return analyticsRepo.getWeekdayAverages(weeks);
    }

    public Map<String, Integer> getPeakHours(final int days) {
        return analyticsRepo.getPeakHours(days);
    }

    public void cleanOldSnapshots(final int daysToKeep) {
        analyticsRepo.cleanOldSnapshots(daysToKeep);
    }

    // === Extended Statistics Methods ===

    public void incrementDeaths(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementDeaths(playerName));
    }

    public void incrementMobKills(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementMobKills(playerName));
    }

    public void incrementPlayerKills(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementPlayerKills(playerName));
    }

    public void incrementBlocksBroken(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementBlocksBroken(playerName));
    }

    public void incrementBlocksPlaced(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementBlocksPlaced(playerName));
    }

    public void incrementMessagesSent(final String playerName) {
        runAsync(() -> playerStatsRepo.incrementMessagesSent(playerName));
    }

    public void updateLastActivity(final String playerName) {
        runAsync(() -> playerStatsRepo.updateLastActivity(playerName));
    }

    public int getPlayerDeaths(final String playerName) {
        return playerStatsRepo.getPlayerDeaths(playerName);
    }

    public int getPlayerMobKills(final String playerName) {
        return playerStatsRepo.getPlayerMobKills(playerName);
    }

    public int getPlayerPlayerKills(final String playerName) {
        return playerStatsRepo.getPlayerPlayerKills(playerName);
    }

    public int getPlayerBlocksBroken(final String playerName) {
        return playerStatsRepo.getPlayerBlocksBroken(playerName);
    }

    public int getPlayerBlocksPlaced(final String playerName) {
        return playerStatsRepo.getPlayerBlocksPlaced(playerName);
    }

    public int getPlayerMessagesSent(final String playerName) {
        return playerStatsRepo.getPlayerMessagesSent(playerName);
    }
}
