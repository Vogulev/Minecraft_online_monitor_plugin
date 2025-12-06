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
 * Repository for time-based online analytics
 */
public class AnalyticsRepository {

    private static final Logger logger = Logger.getLogger("OnlineMonitor");

    public static final String AVG_ONLINE = "avg_online";

    private final ConnectionManager connectionManager;

    public AnalyticsRepository(ConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    public void recordOnlineSnapshot(int onlineCount) {
        String sql = "INSERT INTO online_snapshots (online_count, timestamp) VALUES (?, " + connectionManager.getCurrentTimestamp() + ")";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, onlineCount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.severe("Error recording online snapshot: " + e.getMessage());
        }
    }

    public Map<Integer, Double> getHourlyAverages(int days) {
        Map<Integer, Double> hourlyAvg = new LinkedHashMap<>();
        String sql = """
            SELECT strftime('%H', timestamp) as hour, AVG(online_count) as avg_online
            FROM online_snapshots
            WHERE timestamp >= datetime('now', '-' || ? || ' days')
            GROUP BY hour
            ORDER BY hour
        """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int hour = Integer.parseInt(rs.getString("hour"));
                double avg = rs.getDouble(AVG_ONLINE);
                hourlyAvg.put(hour, avg);
            }
        } catch (SQLException e) {
            logger.severe("Error getting hourly averages: " + e.getMessage());
        }

        return hourlyAvg;
    }

    public Map<String, Double> getDailyAverages(int days) {
        Map<String, Double> dailyAvg = new LinkedHashMap<>();
        String sql = """
            SELECT date(timestamp) as day, AVG(online_count) as avg_online
            FROM online_snapshots
            WHERE timestamp >= datetime('now', '-' || ? || ' days')
            GROUP BY day
            ORDER BY day
        """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String day = rs.getString("day");
                double avg = rs.getDouble(AVG_ONLINE);
                dailyAvg.put(day, avg);
            }
        } catch (SQLException e) {
            logger.severe("Error getting daily averages: " + e.getMessage());
        }

        return dailyAvg;
    }

    public Map<Integer, Double> getWeekdayAverages(int weeks) {
        Map<Integer, Double> weekdayAvg = new LinkedHashMap<>();
        String sql = """
            SELECT
                CAST(strftime('%w', timestamp) AS INTEGER) as weekday_num,
                AVG(online_count) as avg_online
            FROM online_snapshots
            WHERE timestamp >= datetime('now', '-' || ? || ' weeks')
            GROUP BY weekday_num
            ORDER BY weekday_num
        """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, weeks);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int weekdayNum = rs.getInt("weekday_num");
                double avg = rs.getDouble(AVG_ONLINE);
                weekdayAvg.put(weekdayNum, avg);
            }
        } catch (SQLException e) {
            logger.severe("Error getting weekday averages: " + e.getMessage());
        }

        return weekdayAvg;
    }

    public Map<String, Integer> getPeakHours(int days) {
        Map<String, Integer> peakHours = new LinkedHashMap<>();
        String sql = """
            SELECT
                strftime('%H', timestamp) as hour,
                MAX(online_count) as peak_online
            FROM online_snapshots
            WHERE timestamp >= datetime('now', '-' || ? || ' days')
            GROUP BY hour
            ORDER BY peak_online DESC
            LIMIT 5
        """;

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String hour = rs.getString("hour") + ":00";
                int peak = rs.getInt("peak_online");
                peakHours.put(hour, peak);
            }
        } catch (SQLException e) {
            logger.severe("Error getting peak hours: " + e.getMessage());
        }

        return peakHours;
    }

    public void cleanOldSnapshots(int daysToKeep) {
        String sql = "DELETE FROM online_snapshots WHERE timestamp < datetime('now', '-' || ? || ' days')";
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, daysToKeep);
            int deleted = pstmt.executeUpdate();
            if (deleted > 0) {
                logger.info("Cleaned " + deleted + " old snapshots from database");
            }
        } catch (SQLException e) {
            logger.severe("Error cleaning old snapshots: " + e.getMessage());
        }
    }
}
