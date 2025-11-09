package com.vogulev.online_monitor.database.repositories;

import com.vogulev.online_monitor.database.ConnectionManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Репозиторий для работы со статистикой игроков
 */
public class PlayerStatsRepository {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final ConnectionManager connectionManager;

    public PlayerStatsRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void recordPlayerJoin(String playerName) {
        String sql = "INSERT INTO player_stats (player_name, total_joins, first_join, last_join) " +
                "VALUES (?, 1, " + connectionManager.getCurrentTimestamp() + ", " + connectionManager.getCurrentTimestamp() + ") " +
                "ON CONFLICT(player_name) DO UPDATE SET " +
                "total_joins = total_joins + 1, " +
                "last_join = " + connectionManager.getCurrentTimestamp();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error recording player join: " + e.getMessage());
        }
    }

    public void updatePlaytime(String playerName, long sessionDuration) {
        String sql = "UPDATE player_stats SET total_playtime = total_playtime + ? WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sessionDuration);
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error updating player playtime: " + e.getMessage());
        }
    }

    public int getPlayerJoinCount(String playerName) {
        String sql = "SELECT total_joins FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_joins");
            }
        } catch (SQLException e) {
            logger.severe("Error getting player join count: " + e.getMessage());
        }
        return 0;
    }

    public long getPlayerTotalPlaytime(String playerName) {
        String sql = "SELECT total_playtime FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("total_playtime");
            }
        } catch (SQLException e) {
            logger.severe("Error getting player playtime: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getTopPlayersByJoins(int limit) {
        Map<String, Integer> topPlayers = new LinkedHashMap<>();
        String sql = "SELECT player_name, total_joins FROM player_stats ORDER BY total_joins DESC LIMIT ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                topPlayers.put(rs.getString("player_name"), rs.getInt("total_joins"));
            }
        } catch (SQLException e) {
            logger.severe("Error getting top players: " + e.getMessage());
        }

        return topPlayers;
    }

    public long getTotalPlaytime() {
        String sql = "SELECT SUM(total_playtime) as total FROM player_stats";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("total");
            }
        } catch (SQLException e) {
            logger.severe("Error getting total playtime: " + e.getMessage());
        }
        return 0;
    }
}
