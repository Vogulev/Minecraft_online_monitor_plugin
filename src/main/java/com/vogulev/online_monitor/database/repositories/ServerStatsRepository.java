package com.vogulev.online_monitor.database.repositories;

import com.vogulev.online_monitor.database.ConnectionManager;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Repository for working with server statistics (maximum online, unique players)
 */
public class ServerStatsRepository {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final ConnectionManager connectionManager;

    private volatile int cachedMaxOnline = 0;
    private final AtomicInteger cachedUniquePlayersCount = new AtomicInteger(0);
    private volatile boolean cacheInitialized = false;

    public ServerStatsRepository(final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Initializes cache from the database
     */
    private void initializeCache() {
        if (!cacheInitialized) {
            cachedMaxOnline = getMaxOnlineFromDB();
            cachedUniquePlayersCount.set(getUniquePlayersCountFromDB());
            cacheInitialized = true;
            logger.info("Cache initialized: maxOnline=" + cachedMaxOnline + ", uniquePlayers=" + cachedUniquePlayersCount);
        }
    }

    public void updateMaxOnline(final int currentOnline) {
        final String sql = "UPDATE server_stats SET max_online = ? WHERE id = 1 AND max_online < ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentOnline);
            pstmt.setInt(2, currentOnline);
            pstmt.executeUpdate();

            if (currentOnline > cachedMaxOnline) {
                cachedMaxOnline = currentOnline;
            }
        } catch (final SQLException e) {
            logger.severe("Error updating max online: " + e.getMessage());
        }
    }

    private int getMaxOnlineFromDB() {
        final String sql = "SELECT max_online FROM server_stats WHERE id = 1";
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("max_online");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting max online: " + e.getMessage());
        }
        return 0;
    }

    public int getMaxOnline() {
        initializeCache(); // Lazy initialization
        return cachedMaxOnline;
    }

    public void incrementUniquePlayer() {
        final String sql = "UPDATE server_stats SET total_unique_players = total_unique_players + 1 WHERE id = 1";
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);

            cachedUniquePlayersCount.incrementAndGet();
        } catch (final SQLException e) {
            logger.severe("Error incrementing unique players: " + e.getMessage());
        }
    }

    private int getUniquePlayersCountFromDB() {
        final String sql = "SELECT total_unique_players FROM server_stats WHERE id = 1";
        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total_unique_players");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting unique players count: " + e.getMessage());
        }
        return 0;
    }

    public int getUniquePlayersCount() {
        initializeCache();
        return cachedUniquePlayersCount.get();
    }
}
