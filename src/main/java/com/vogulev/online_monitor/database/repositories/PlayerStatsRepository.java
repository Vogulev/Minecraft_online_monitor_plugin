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
 * Repository for working with player statistics
 */
public class PlayerStatsRepository {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private final ConnectionManager connectionManager;

    public PlayerStatsRepository(final ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void recordPlayerJoin(final String playerName) {
        final String sql = "INSERT INTO player_stats (player_name, total_joins, first_join, last_join) " +
                "VALUES (?, 1, " + connectionManager.getCurrentTimestamp() + ", " + connectionManager.getCurrentTimestamp() + ") " +
                "ON CONFLICT(player_name) DO UPDATE SET " +
                "total_joins = total_joins + 1, " +
                "last_join = " + connectionManager.getCurrentTimestamp();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error recording player join: " + e.getMessage());
        }
    }

    public void updatePlaytime(final String playerName, final long sessionDuration) {
        final String sql = "UPDATE player_stats SET total_playtime = total_playtime + ? WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setLong(1, sessionDuration);
            pstmt.setString(2, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error updating player playtime: " + e.getMessage());
        }
    }

    public int getPlayerJoinCount(final String playerName) {
        final String sql = "SELECT total_joins FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("total_joins");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player join count: " + e.getMessage());
        }
        return 0;
    }

    public long getPlayerTotalPlaytime(final String playerName) {
        final String sql = "SELECT total_playtime FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong("total_playtime");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player playtime: " + e.getMessage());
        }
        return 0;
    }

    public Map<String, Integer> getTopPlayersByJoins(final int limit) {
        final Map<String, Integer> topPlayers = new LinkedHashMap<>();
        final String sql = "SELECT player_name, total_joins FROM player_stats ORDER BY total_joins DESC LIMIT ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            final ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                topPlayers.put(rs.getString("player_name"), rs.getInt("total_joins"));
            }
        } catch (final SQLException e) {
            logger.severe("Error getting top players: " + e.getMessage());
        }

        return topPlayers;
    }

    public long getTotalPlaytime() {
        final String sql = "SELECT SUM(total_playtime) as total FROM player_stats";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getLong("total");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting total playtime: " + e.getMessage());
        }
        return 0;
    }

    // Extended statistics methods

    public void incrementDeaths(final String playerName) {
        final String sql = "UPDATE player_stats SET deaths = deaths + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing deaths: " + e.getMessage());
        }
    }

    public void incrementMobKills(final String playerName) {
        final String sql = "UPDATE player_stats SET mob_kills = mob_kills + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing mob kills: " + e.getMessage());
        }
    }

    public void incrementPlayerKills(final String playerName) {
        final String sql = "UPDATE player_stats SET player_kills = player_kills + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing player kills: " + e.getMessage());
        }
    }

    public void incrementBlocksBroken(final String playerName) {
        final String sql = "UPDATE player_stats SET blocks_broken = blocks_broken + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing blocks broken: " + e.getMessage());
        }
    }

    public void incrementBlocksPlaced(final String playerName) {
        final String sql = "UPDATE player_stats SET blocks_placed = blocks_placed + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing blocks placed: " + e.getMessage());
        }
    }

    public void incrementMessagesSent(final String playerName) {
        final String sql = "UPDATE player_stats SET messages_sent = messages_sent + 1 WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error incrementing messages sent: " + e.getMessage());
        }
    }

    public void updateLastActivity(final String playerName) {
        final String sql = "UPDATE player_stats SET last_activity = " + connectionManager.getCurrentTimestamp() + " WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            pstmt.executeUpdate();
        } catch (final SQLException e) {
            logger.severe("Error updating last activity: " + e.getMessage());
        }
    }

    public int getPlayerDeaths(final String playerName) {
        final String sql = "SELECT deaths FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("deaths");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player deaths: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayerMobKills(final String playerName) {
        final String sql = "SELECT mob_kills FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("mob_kills");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player mob kills: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayerPlayerKills(final String playerName) {
        final String sql = "SELECT player_kills FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("player_kills");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player kills: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayerBlocksBroken(final String playerName) {
        final String sql = "SELECT blocks_broken FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("blocks_broken");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player blocks broken: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayerBlocksPlaced(final String playerName) {
        final String sql = "SELECT blocks_placed FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("blocks_placed");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player blocks placed: " + e.getMessage());
        }
        return 0;
    }

    public int getPlayerMessagesSent(final String playerName) {
        final String sql = "SELECT messages_sent FROM player_stats WHERE player_name = ?";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, playerName);
            final ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("messages_sent");
            }
        } catch (final SQLException e) {
            logger.severe("Error getting player messages sent: " + e.getMessage());
        }
        return 0;
    }
}
