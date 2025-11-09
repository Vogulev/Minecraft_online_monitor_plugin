package com.vogulev.online_monitor.database.repositories;

import com.vogulev.online_monitor.database.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Репозиторий для работы с сессиями игроков
 */
public class SessionRepository {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final ConnectionManager connectionManager;

    public SessionRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void createSession(String playerName) {
        String sql = "INSERT INTO player_sessions (player_name, join_time) VALUES (?, " + connectionManager.getCurrentTimestamp() + ")";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error creating player session: " + e.getMessage());
        }
    }

    public void closeSession(String playerName, long sessionDuration) {
        try (Connection conn = connectionManager.getConnection()) {
            // Находим активную сессию
            String findSessionSql = "SELECT id FROM player_sessions " +
                    "WHERE player_name = ? AND quit_time IS NULL " +
                    "ORDER BY join_time DESC LIMIT 1";

            int sessionId = -1;
            try (PreparedStatement findStmt = conn.prepareStatement(findSessionSql)) {
                findStmt.setString(1, playerName);
                ResultSet rs = findStmt.executeQuery();
                if (rs.next()) {
                    sessionId = rs.getInt("id");
                }
            }

            if (sessionId != -1) {
                // Обновляем сессию
                String updateSql = "UPDATE player_sessions " +
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

        } catch (SQLException e) {
            logger.severe("Error closing player session: " + e.getMessage());
        }
    }

    public int getTotalSessions() {
        String sql = "SELECT COUNT(*) as total FROM player_sessions";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        } catch (SQLException e) {
            logger.severe("Error getting total sessions: " + e.getMessage());
        }
        return 0;
    }

    public int getActiveSessions() {
        String sql = "SELECT COUNT(*) as active FROM player_sessions WHERE quit_time IS NULL";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("active");
            }
        } catch (SQLException e) {
            logger.severe("Error getting active sessions: " + e.getMessage());
        }
        return 0;
    }
}
