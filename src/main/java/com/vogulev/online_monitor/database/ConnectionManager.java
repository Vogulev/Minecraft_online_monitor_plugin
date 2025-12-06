package com.vogulev.online_monitor.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Database connection management through HikariCP connection pool
 */
public class ConnectionManager {

    private static final Logger logger = Logger.getLogger("OnlineMonitor");

    public static final String MYSQL = "mysql";

    private HikariDataSource dataSource;
    private String databaseType = "sqlite";
    private String timezoneModifier = "";

    public void connect(String type, String host, int port, String database, String username, String password, File dataFolder) throws SQLException {
        this.databaseType = type.toLowerCase();

        HikariConfig config = new HikariConfig();

        if (MYSQL.equals(databaseType)) {
            config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            logger.info("Connecting to MySQL database: " + host + ":" + port + "/" + database);
        } else {
            if (!dataFolder.exists()) {
                dataFolder.mkdirs();
            }
            File dbFile = new File(dataFolder, "statistics.db");
            config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            config.setDriverClassName("org.sqlite.JDBC");
            logger.info("Connecting to SQLite database: " + dbFile.getAbsolutePath());
        }

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

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void setTimezoneOffset(String offset) {
        if (offset != null && !offset.isEmpty()) {
            this.timezoneModifier = ", '" + offset + " hours'";
            logger.info("Timezone set to UTC" + offset + " (Moscow Time)");
        }
    }

    public String getCurrentTimestamp() {
        if (MYSQL.equals(databaseType)) {
            return "DATE_ADD(NOW(), INTERVAL " + timezoneModifier.replace(", '", "").replace(" hours'", "") + " HOUR)";
        }
        return "datetime('now'" + timezoneModifier + ")";
    }

    private void createTables() throws SQLException {
        String autoIncrement = MYSQL.equals(databaseType) ? "AUTO_INCREMENT" : "AUTOINCREMENT";

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

            String initServerStats = "mysql".equals(databaseType) ?
                "INSERT IGNORE INTO server_stats (id, max_online, total_unique_players) VALUES (1, 0, 0)" :
                "INSERT OR IGNORE INTO server_stats (id, max_online, total_unique_players) VALUES (1, 0, 0)";
            stmt.execute(initServerStats);

            logger.info("Database tables created/verified successfully");
        }
    }
}