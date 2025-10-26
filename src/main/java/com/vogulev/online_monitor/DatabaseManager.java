package com.vogulev.online_monitor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

public class DatabaseManager {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private HikariDataSource dataSource;
    private final File dataFolder;
    private String timezoneModifier = "";
    private String databaseType = "sqlite";
    private Plugin plugin;

    private volatile int cachedMaxOnline = 0;
    private volatile int cachedUniquePlayersCount = 0;
    private volatile boolean cacheInitialized = false;

    public DatabaseManager(File dataFolder) {
        this.dataFolder = dataFolder;
    }

    public void setPlugin(Plugin plugin) {
        this.plugin = plugin;
    }

    public void setTimezoneOffset(String offset) {
        if (offset != null && !offset.isEmpty()) {
            this.timezoneModifier = ", '" + offset + " hours'";
            logger.info("Timezone set to UTC" + offset + " (Moscow Time)");
        }
    }

    public void connect(String type, String host, int port, String database, String username, String password) throws SQLException {
        this.databaseType = type.toLowerCase();

        HikariConfig config = new HikariConfig();

        if ("mysql".equals(databaseType)) {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            logger.info("Connecting to MySQL database: " + host + ":" + port + "/" + database);
        } else {
            // SQLite
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "statistics.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            logger.info("Connecting to SQLite database: " + dbFile.getAbsolutePath());
        }

        // Connection pool settings
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        dataSource = new HikariDataSource(config);

        createTables();
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }

    private Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    private void createTables() throws SQLException {
        String autoIncrement = "mysql".equals(databaseType) ? "AUTO_INCREMENT" : "AUTOINCREMENT";

        String serverStatsTable =
            "CREATE TABLE IF NOT EXISTS server_stats (" +
            "id INTEGER PRIMARY KEY CHECK (id = 1)," +
            "max_online INTEGER DEFAULT 0," +
            "total_unique_players INTEGER DEFAULT 0," +
            "created_at TIMESTAMP" +
            ")";

        String playerStatsTable =
            "CREATE TABLE IF NOT EXISTS player_stats (" +
            "player_name VARCHAR(16) PRIMARY KEY," +
            "total_joins INTEGER DEFAULT 0," +
            "total_playtime BIGINT DEFAULT 0," +
            "first_join TIMESTAMP," +
            "last_join TIMESTAMP" +
            ")";

        String playerSessionsTable =
            "CREATE TABLE IF NOT EXISTS player_sessions (" +
            "id INTEGER PRIMARY KEY " + autoIncrement + "," +
            "player_name VARCHAR(16) NOT NULL," +
            "join_time TIMESTAMP NOT NULL," +
            "quit_time TIMESTAMP," +
            "session_duration BIGINT DEFAULT 0" +
            ")";

        String onlineSnapshotsTable =
            "CREATE TABLE IF NOT EXISTS online_snapshots (" +
            "id INTEGER PRIMARY KEY " + autoIncrement + "," +
            "online_count INTEGER NOT NULL," +
            "timestamp TIMESTAMP" +
            ")";

        String snapshotIndexSql = "CREATE INDEX IF NOT EXISTS idx_snapshots_timestamp ON online_snapshots(timestamp)";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(serverStatsTable);
            stmt.execute(playerStatsTable);
            stmt.execute(playerSessionsTable);
            stmt.execute(onlineSnapshotsTable);
            stmt.execute(snapshotIndexSql);

            // Инициализация server_stats если пусто
            String initServerStats = "mysql".equals(databaseType) ?
                "INSERT IGNORE INTO server_stats (id, max_online, total_unique_players) VALUES (1, 0, 0)" :
                "INSERT OR IGNORE INTO server_stats (id, max_online, total_unique_players) VALUES (1, 0, 0)";
            stmt.execute(initServerStats);

            logger.info("Database tables created/verified successfully");
        }
    }

    private String getCurrentTimestamp() {
        if ("mysql".equals(databaseType)) {
            // MySQL использует NOW() + INTERVAL для смещения времени
            return "DATE_ADD(NOW(), INTERVAL " + timezoneModifier.replace(", '", "").replace(" hours'", "") + " HOUR)";
        }
        return "datetime('now'" + timezoneModifier + ")";
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

    /**
     * Инициализирует кэш из базы данных.
     * Вызывается один раз при подключении к БД.
     */
    private void initializeCache() {
        if (!cacheInitialized) {
            cachedMaxOnline = getMaxOnlineFromDB();
            cachedUniquePlayersCount = getUniquePlayersCountFromDB();
            cacheInitialized = true;
            logger.info("Cache initialized: maxOnline=" + cachedMaxOnline + ", uniquePlayers=" + cachedUniquePlayersCount);
        }
    }

    // === Server Stats Methods ===

    public void updateMaxOnline(int currentOnline) {
        runAsync(() -> {
            String sql = "UPDATE server_stats SET max_online = ? WHERE id = 1 AND max_online < ?";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, currentOnline);
                pstmt.setInt(2, currentOnline);
                pstmt.executeUpdate();

                // Обновляем кэш после записи
                if (currentOnline > cachedMaxOnline) {
                    cachedMaxOnline = currentOnline;
                }
            } catch (SQLException e) {
                logger.severe("Error updating max online: " + e.getMessage());
            }
        });
    }

    private int getMaxOnlineFromDB() {
        String sql = "SELECT max_online FROM server_stats WHERE id = 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("max_online");
            }
        } catch (SQLException e) {
            logger.severe("Error getting max online: " + e.getMessage());
        }
        return 0;
    }

    public int getMaxOnline() {
        initializeCache(); // Lazy initialization
        return cachedMaxOnline;
    }

    public void incrementUniquePlayer() {
        runAsync(() -> {
            String sql = "UPDATE server_stats SET total_unique_players = total_unique_players + 1 WHERE id = 1";
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute(sql);

                // Обновляем кэш после записи
                cachedUniquePlayersCount++;
            } catch (SQLException e) {
                logger.severe("Error incrementing unique players: " + e.getMessage());
            }
        });
    }

    private int getUniquePlayersCountFromDB() {
        String sql = "SELECT total_unique_players FROM server_stats WHERE id = 1";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("total_unique_players");
            }
        } catch (SQLException e) {
            logger.severe("Error getting unique players count: " + e.getMessage());
        }
        return 0;
    }

    public int getUniquePlayersCount() {
        initializeCache(); // Lazy initialization
        return cachedUniquePlayersCount;
    }

    // === Player Stats Methods ===

    public void recordPlayerJoin(String playerName) {
        runAsync(() -> {
            String sql = "INSERT INTO player_stats (player_name, total_joins, first_join, last_join) " +
                    "VALUES (?, 1, " + getCurrentTimestamp() + ", " + getCurrentTimestamp() + ") " +
                    "ON CONFLICT(player_name) DO UPDATE SET " +
                    "total_joins = total_joins + 1, " +
                    "last_join = " + getCurrentTimestamp();

            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, playerName);
                pstmt.executeUpdate();

                // Создаем запись о начале сессии
                String sessionSql = "INSERT INTO player_sessions (player_name, join_time) VALUES (?, " + getCurrentTimestamp() + ")";
                try (PreparedStatement sessionStmt = conn.prepareStatement(sessionSql)) {
                    sessionStmt.setString(1, playerName);
                    sessionStmt.executeUpdate();
                }

            } catch (SQLException e) {
                logger.severe("Error recording player join: " + e.getMessage());
            }
        });
    }

    public void recordPlayerQuit(String playerName, long sessionDuration) {
        runAsync(() -> {
            try (Connection conn = getConnection()) {
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
                    String updateSql = "UPDATE player_sessions " +
                            "SET quit_time = " + getCurrentTimestamp() + ", " +
                            "session_duration = ? " +
                            "WHERE id = ?";

                    try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                        updateStmt.setLong(1, sessionDuration);
                        updateStmt.setInt(2, sessionId);
                        updateStmt.executeUpdate();
                    }

                    // Обновляем общее время игры
                    String updatePlaytime = "UPDATE player_stats SET total_playtime = total_playtime + ? WHERE player_name = ?";
                    try (PreparedStatement playtimeStmt = conn.prepareStatement(updatePlaytime)) {
                        playtimeStmt.setLong(1, sessionDuration);
                        playtimeStmt.setString(2, playerName);
                        playtimeStmt.executeUpdate();
                    }
                } else {
                    logger.warning("No active session found for player: " + playerName);
                }

            } catch (SQLException e) {
                logger.severe("Error recording player quit: " + e.getMessage());
            }
        });
    }

    public int getPlayerJoinCount(String playerName) {
        String sql = "SELECT total_joins FROM player_stats WHERE player_name = ?";
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getLong("total");
            }
        } catch (SQLException e) {
            logger.severe("Error getting total playtime: " + e.getMessage());
        }
        return 0;
    }

    public int getTotalSessions() {
        String sql = "SELECT COUNT(*) as total FROM player_sessions";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
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
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt("active");
            }
        } catch (SQLException e) {
            logger.severe("Error getting active sessions: " + e.getMessage());
        }
        return 0;
    }

    // === Time Analytics Methods ===

    public void recordOnlineSnapshot(int onlineCount) {
        runAsync(() -> {
            String sql = "INSERT INTO online_snapshots (online_count, timestamp) VALUES (?, " + getCurrentTimestamp() + ")";
            try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setInt(1, onlineCount);
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.severe("Error recording online snapshot: " + e.getMessage());
            }
        });
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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                int hour = Integer.parseInt(rs.getString("hour"));
                double avg = rs.getDouble("avg_online");
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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, days);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String day = rs.getString("day");
                double avg = rs.getDouble("avg_online");
                dailyAvg.put(day, avg);
            }
        } catch (SQLException e) {
            logger.severe("Error getting daily averages: " + e.getMessage());
        }

        return dailyAvg;
    }

    public Map<String, Double> getWeekdayAverages(int weeks) {
        Map<String, Double> weekdayAvg = new LinkedHashMap<>();
        String sql = """
            SELECT
                CASE CAST(strftime('%w', timestamp) AS INTEGER)
                    WHEN 0 THEN 'Воскресенье'
                    WHEN 1 THEN 'Понедельник'
                    WHEN 2 THEN 'Вторник'
                    WHEN 3 THEN 'Среда'
                    WHEN 4 THEN 'Четверг'
                    WHEN 5 THEN 'Пятница'
                    WHEN 6 THEN 'Суббота'
                END as weekday,
                strftime('%w', timestamp) as weekday_num,
                AVG(online_count) as avg_online
            FROM online_snapshots
            WHERE timestamp >= datetime('now', '-' || ? || ' weeks')
            GROUP BY weekday_num
            ORDER BY weekday_num
        """;

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, weeks);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String weekday = rs.getString("weekday");
                double avg = rs.getDouble("avg_online");
                weekdayAvg.put(weekday, avg);
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

        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
