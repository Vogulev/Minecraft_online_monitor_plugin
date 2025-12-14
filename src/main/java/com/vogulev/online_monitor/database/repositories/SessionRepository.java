package com.vogulev.online_monitor.database.repositories;

import com.vogulev.online_monitor.database.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Repository for working with player sessions
 */
public class SessionRepository {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final ConnectionManager connectionManager;

    public SessionRepository(final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void createSession(final String playerName) {
        final String sql = "INSERT INTO player_sessions (player_name, join_time) VALUES (?, " + connectionManager.getCurrentTimestamp() + ")";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error creating player session: " + e.getMessage());
        }
    }

    public void closeSession(final String playerName, final long sessionDuration) {
        try (Connection conn = connectionManager.getConnection()) {
            // Find active session
            final String findSessionSql = "SELECT id FROM player_sessions " +
                    "WHERE player_name = ? AND quit_time IS NULL " +
                    "ORDER BY join_time DESC LIMIT 1";

            int sessionId = -1;
            try (PreparedStatement findStmt = conn.prepareStatement(findSessionSql)) {
                findStmt.setString(1, playerName);
                final ResultSet rs = findStmt.executeQuery();
                if (rs.next()) {
                    sessionId = rs.getInt("id");
                }
            }

            if (sessionId != -1) {
                // Update session
                final String updateSql = "UPDATE player_sessions " +
                        "SET quit_time = " + connectionManager.getCurrentTimestamp() + ", " +
                        "session_duration = ? " +
                        "WHERE id = ?";

                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setLong(1, sessionDuration);
                    updateStmt.setInt(2, sessionId);
                    updateStmt.executeUpdate();
                }
            } else {
                logger.warning("No active session found for player: " + playerName);
            }

        } catch (final SQLException e) {
            logger.severe("Error closing player session: " + e.getMessage());
        }
    }

    public int getTotalSessions() {
        final String sql = "SELECT COUNT(*) as total FROM player_sessions";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting total sessions: " + e.getMessage());
        }
        return 0;
    }

    public int getActiveSessions() {
        final String sql = "SELECT COUNT(*) as active FROM player_sessions WHERE quit_time IS NULL";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("active");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting active sessions: " + e.getMessage());
        }
        return 0;
    }
}
