package com.vogulev.online_monitor.database;


import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;


/**
 * Database connection management through HikariCP connection pool
 */
public class ConnectionManager
{

    private static final Logger logger = Logger.getLogger("OnlineMonitor");

    public static final String MYSQL = "mysql";

    private String databaseType = "sqlite";

    private HikariDataSource dataSource;

    private String timezoneModifier = "";


    /**
     * Connect to database using settings from config.yml
     * All HikariCP settings are automatically loaded from database.hikari section
     * @param config FileConfiguration from plugin's config.yml
     * @param dataFolder Plugin data folder for SQLite database file
     */
    public void connect(final FileConfiguration config, final File dataFolder)
    {
        this.databaseType = config.getString("database.type", "sqlite").toLowerCase();

        final Properties props = loadHikariProperties(config);

        String jdbcUrl = props.getProperty("jdbcUrl");
        if (jdbcUrl == null || jdbcUrl.isEmpty())
        {
            jdbcUrl = buildJdbcUrl(config, dataFolder);
            props.setProperty("jdbcUrl", jdbcUrl);
        }

        if (!props.containsKey("driverClassName"))
        {
            final String driver = MYSQL.equals(databaseType)
                ? "com.mysql.cj.jdbc.Driver"
                : "org.sqlite.JDBC";
            props.setProperty("driverClassName", driver);
        }

        logger.info("Connecting to " + databaseType.toUpperCase() + " database: " + jdbcUrl);

        final HikariConfig hikariConfig = new HikariConfig(props);
        dataSource = new HikariDataSource(hikariConfig);

        runMigrations();
    }


    /**
     * Automatically load all HikariCP properties from database.hikari section
     * Any property set in config.yml will be passed to HikariCP
     */
    private Properties loadHikariProperties(final FileConfiguration config)
    {
        final Properties props = new Properties();

        final ConfigurationSection hikariSection = config.getConfigurationSection("database.hikari");

        if (hikariSection != null)
        {
            for (final String key : hikariSection.getKeys(false))
            {
                final Object value = hikariSection.get(key);
                if (value != null)
                {
                    props.setProperty(key, String.valueOf(value));
                }
            }
        }
        return props;
    }


    /**
     * Auto-build JDBC URL based on database type
     * Reads settings from database.mysql or database.sqlite sections
     */
    private String buildJdbcUrl(final FileConfiguration config, final File dataFolder)
    {
        if (MYSQL.equals(databaseType))
        {
            final String host = config.getString("database.mysql.host", "localhost");
            final int port = config.getInt("database.mysql.port", 3306);
            final String database = config.getString("database.mysql.database", "minecraft_stats");
            final String timezone = config.getString("database.mysql.timezone", "UTC");

            return "jdbc:mysql://" + host + ":" + port + "/" + database +
                   "?useSSL=false&serverTimezone=" + timezone;
        }
        else
        {
            final String filename = config.getString("database.sqlite.filename", "statistics.db");

            if (!dataFolder.exists())
            {
                dataFolder.mkdirs();
            }

            final File dbFile = new File(dataFolder, filename);
            return "jdbc:sqlite:" + dbFile.getAbsolutePath();
        }
    }


    public void disconnect()
    {
        if (dataSource != null && !dataSource.isClosed())
        {
            dataSource.close();
            logger.info("Database connection pool closed");
        }
    }


    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }


    public String getDatabaseType()
    {
        return databaseType;
    }


    public void setTimezoneOffset(final String offset)
    {
        if (offset != null && !offset.isEmpty())
        {
            this.timezoneModifier = ", '" + offset + " hours'";
            logger.info("Timezone set to UTC" + offset + " (Moscow Time)");
        }
    }


    public String getCurrentTimestamp()
    {
        if (MYSQL.equals(databaseType))
        {
            return "DATE_ADD(NOW(), INTERVAL " + timezoneModifier
                .replace(", '", "")
                .replace(" hours'", "") + " HOUR)";
        }
        return "datetime('now'" + timezoneModifier + ")";
    }


    /**
     * Run database migrations using Flyway
     * Automatically applies all pending migrations from classpath:db/migration folder
     * Baseline migration is enabled for existing databases
     */
    private void runMigrations()
    {
        try
        {
            logger.info("Starting database migration with Flyway...");

            final ClassLoader classLoader = getClass().getClassLoader();
            final Flyway flyway = Flyway.configure(classLoader)
                .dataSource(dataSource)
                .load();

            final MigrateResult result = flyway.migrate();

            if (result.migrationsExecuted > 0)
            {
                logger.info(String.format("Successfully applied %d migration(s)", result.migrationsExecuted));
                final String version = result.targetSchemaVersion != null ? result.targetSchemaVersion : "baseline";
                logger.info(String.format("Current schema version: %s", version));
            }
            else
            {
                logger.info("Database schema is up to date - no migrations needed");
                final String currentVersion = flyway.info().current() != null
                    ? flyway.info().current().getVersion().toString()
                    : "baseline";
                logger.info(String.format("Current schema version: %s", currentVersion));
            }

        }
        catch (final Exception e)
        {
            logger.severe(String.format("Failed to run database migrations: %s", e.getMessage()));
            throw new RuntimeException("Database migration failed. Check database configuration and migrations.", e);
        }
    }
}
