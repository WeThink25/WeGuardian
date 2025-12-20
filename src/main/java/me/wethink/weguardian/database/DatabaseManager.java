package me.wethink.weguardian.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.wethink.weguardian.WeGuardian;
import org.bukkit.configuration.ConfigurationSection;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DatabaseManager {

    private final WeGuardian plugin;
    private HikariDataSource dataSource;
    private String databaseType;

    public DatabaseManager(WeGuardian plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        databaseType = plugin.getConfig().getString("database.type", "sqlite").toLowerCase();

        plugin.getLogger().info("Initializing database type: " + databaseType);

        try {
            HikariConfig config;

            switch (databaseType) {
                case "mysql" -> config = initializeMySQL();
                case "sqlite" -> config = initializeSQLite();
                default -> {
                    plugin.getLogger().warning("Unknown database type '" + databaseType + "', defaulting to SQLite");
                    databaseType = "sqlite";
                    config = initializeSQLite();
                }
            }

            dataSource = new HikariDataSource(config);
            plugin.getLogger()
                    .info("Database connection pool initialized successfully (" + databaseType.toUpperCase() + ")");
            initializeTables();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize database connection pool", e);
        }
    }

    private HikariConfig initializeSQLite() {
        String dbFileName = plugin.getConfig().getString("database.sqlite.file", "punishments.db");
        File dbFile = new File(plugin.getDataFolder(), dbFileName);

        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found!", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setPoolName("WeGuardian-SQLite-Pool");
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 5));
        config.setMinimumIdle(1);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("busy_timeout", "5000");
        config.addDataSourceProperty("synchronous", "NORMAL");
        config.addDataSourceProperty("cache_size", "-20000");
        config.addDataSourceProperty("foreign_keys", "ON");

        return config;
    }

    private HikariConfig initializeMySQL() {
        String host = plugin.getConfig().getString("database.mysql.host", "localhost");
        int port = plugin.getConfig().getInt("database.mysql.port", 3306);
        String database = plugin.getConfig().getString("database.mysql.database", "weguardian");
        String username = plugin.getConfig().getString("database.mysql.username", "root");
        String password = plugin.getConfig().getString("database.mysql.password", "password");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found!", e);
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database);
        config.setUsername(username);
        config.setPassword(password);
        config.setPoolName("WeGuardian-MySQL-Pool");
        config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);

        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");

        ConfigurationSection properties = plugin.getConfig().getConfigurationSection("database.mysql.properties");
        if (properties != null) {
            for (String key : properties.getKeys(false)) {
                config.addDataSourceProperty(key, properties.get(key).toString());
            }
        }

        return config;
    }

    private void initializeTables() {
        String autoIncrement = databaseType.equals("mysql") ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String textType = databaseType.equals("mysql") ? "VARCHAR(255)" : "TEXT";
        String bigintType = databaseType.equals("mysql") ? "BIGINT" : "INTEGER";

        String createPunishmentsTable = String.format("""
                    CREATE TABLE IF NOT EXISTS punishments (
                        id INTEGER PRIMARY KEY %s,
                        target_uuid %s NOT NULL,
                        target_name %s NOT NULL,
                        target_ip %s,
                        staff_uuid %s,
                        staff_name %s NOT NULL,
                        type %s NOT NULL,
                        reason TEXT,
                        created_at %s NOT NULL,
                        expires_at %s,
                        active TINYINT NOT NULL DEFAULT 1,
                        removed_by_uuid %s,
                        removed_by_name %s,
                        removed_at %s,
                        remove_reason TEXT
                    )
                """, autoIncrement, textType, textType, textType, textType, textType,
                textType, bigintType, bigintType, textType, textType, bigintType);

        String createPlayerIpsTable = String.format("""
                    CREATE TABLE IF NOT EXISTS player_ips (
                        uuid %s PRIMARY KEY,
                        name %s NOT NULL,
                        ip_address %s NOT NULL,
                        last_seen %s NOT NULL
                    )
                """, textType, textType, textType, bigintType);

        executeAsync(createPunishmentsTable)
                .thenCompose(v -> executeAsync(createPlayerIpsTable))
                .thenAccept(v -> {
                    createIndexSafe("idx_punishments_target_uuid", "punishments", "target_uuid");
                    createIndexSafe("idx_punishments_active", "punishments", "active, type");
                    createIndexSafe("idx_punishments_expires", "punishments", "expires_at");
                    createIndexSafe("idx_punishments_target_ip", "punishments", "target_ip");
                })
                .exceptionally(e -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to initialize database tables", e);
                    return null;
                });
    }

    private void createIndexSafe(String indexName, String tableName, String columns) {
        String sql;
        if (databaseType.equals("mysql")) {
            sql = String.format("CREATE INDEX %s ON %s(%s)", indexName, tableName, columns);
        } else {
            sql = String.format("CREATE INDEX IF NOT EXISTS %s ON %s(%s)", indexName, tableName, columns);
        }

        try (Connection conn = getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.execute();
        } catch (SQLException e) {
            if (databaseType.equals("mysql") && e.getMessage() != null
                    && e.getMessage().contains("Duplicate key name")) {
                return;
            }
            plugin.getLogger().log(Level.WARNING, "Failed to create index " + indexName + ": " + e.getMessage());
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("Database not initialized");
        }
        return dataSource.getConnection();
    }

    public CompletableFuture<Void> executeAsync(String sql) {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.execute();
            } catch (SQLException e) {
                throw new RuntimeException("Failed to execute SQL", e);
            }
        });
    }

    public boolean isConnected() {
        return dataSource != null && !dataSource.isClosed();
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            plugin.getLogger().info("Database connection pool closed.");
        }
    }
}
