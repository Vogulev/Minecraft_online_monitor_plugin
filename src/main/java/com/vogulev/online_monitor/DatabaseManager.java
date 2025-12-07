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

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void setTimezoneOffset(String offset) {
        if (connectionManager != null) {
            connectionManager.setTimezoneOffset(offset);
        }
    }

    public void connect(org.bukkit.configuration.file.FileConfiguration config) throws SQLException {
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
    private CompletableFuture<Void> runAsync(Runnable task) {
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
    public void updateMaxOnline(int currentOnline) {
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
    public void recordPlayerJoin(String playerName) {
        runAsync(() -> {
            playerStatsRepo.recordPlayerJoin(playerName);
            sessionRepo.createSession(playerName);
        });
    }

    public void recordPlayerQuit(String playerName, long sessionDuration) {
        runAsync(() -> {
            sessionRepo.closeSession(playerName, sessionDuration);
            playerStatsRepo.updatePlaytime(playerName, sessionDuration);
        });
    }

    public int getPlayerJoinCount(String playerName) {
        return playerStatsRepo.getPlayerJoinCount(playerName);
    }

    public long getPlayerTotalPlaytime(String playerName) {
        return playerStatsRepo.getPlayerTotalPlaytime(playerName);
    }

    public Map<String, Integer> getTopPlayersByJoins(int limit) {
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

    public void recordOnlineSnapshot(int onlineCount) {
        runAsync(() -> analyticsRepo.recordOnlineSnapshot(onlineCount));
    }

    public Map<Integer, Double> getHourlyAverages(int days) {
        return analyticsRepo.getHourlyAverages(days);
    }

    public Map<String, Double> getDailyAverages(int days) {
        return analyticsRepo.getDailyAverages(days);
    }

    public Map<Integer, Double> getWeekdayAverages(int weeks) {
        return analyticsRepo.getWeekdayAverages(weeks);
    }

    public Map<String, Integer> getPeakHours(int days) {
        return analyticsRepo.getPeakHours(days);
    }

    public void cleanOldSnapshots(int daysToKeep) {
        analyticsRepo.cleanOldSnapshots(daysToKeep);
    }

    // === Extended Statistics Methods ===

    public void incrementDeaths(String playerName) {
        runAsync(() -> playerStatsRepo.incrementDeaths(playerName));
    }

    public void incrementMobKills(String playerName) {
        runAsync(() -> playerStatsRepo.incrementMobKills(playerName));
    }

    public void incrementPlayerKills(String playerName) {
        runAsync(() -> playerStatsRepo.incrementPlayerKills(playerName));
    }

    public void incrementBlocksBroken(String playerName) {
        runAsync(() -> playerStatsRepo.incrementBlocksBroken(playerName));
    }

    public void incrementBlocksPlaced(String playerName) {
        runAsync(() -> playerStatsRepo.incrementBlocksPlaced(playerName));
    }

    public void incrementMessagesSent(String playerName) {
        runAsync(() -> playerStatsRepo.incrementMessagesSent(playerName));
    }

    public void updateLastActivity(String playerName) {
        runAsync(() -> playerStatsRepo.updateLastActivity(playerName));
    }

    public int getPlayerDeaths(String playerName) {
        return playerStatsRepo.getPlayerDeaths(playerName);
    }

    public int getPlayerMobKills(String playerName) {
        return playerStatsRepo.getPlayerMobKills(playerName);
    }

    public int getPlayerPlayerKills(String playerName) {
        return playerStatsRepo.getPlayerPlayerKills(playerName);
    }

    public int getPlayerBlocksBroken(String playerName) {
        return playerStatsRepo.getPlayerBlocksBroken(playerName);
    }

    public int getPlayerBlocksPlaced(String playerName) {
        return playerStatsRepo.getPlayerBlocksPlaced(playerName);
    }

    public int getPlayerMessagesSent(String playerName) {
        return playerStatsRepo.getPlayerMessagesSent(playerName);
    }
}
