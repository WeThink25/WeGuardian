package me.wethink.weGuardian.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.BanwaveEntry;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

public class HikariDatabaseManager implements DatabaseManager {

    private final WeGuardian plugin;
    private final boolean useMySQL;
    private final ExecutorService executorService;
    private HikariDataSource dataSource;

    public HikariDatabaseManager(WeGuardian plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newFixedThreadPool(2);
        FileConfiguration config = plugin.getConfig();
        this.useMySQL = config.getBoolean("database.mysql.enabled", false);
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return connect().thenCompose(success -> {
            if (success) {
                return createTables();
            } else {
                return CompletableFuture.failedFuture(new RuntimeException("Failed to connect to database"));
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                HikariConfig config = new HikariConfig();

                if (useMySQL) {
                    setupMySQLConfig(config);
                } else {
                    setupSQLiteConfig(config);
                }

                dataSource = new HikariDataSource(config);

                try (Connection conn = dataSource.getConnection()) {
                    return conn.isValid(5);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
                return false;
            }
        });
    }

    private void setupMySQLConfig(HikariConfig config) {
        FileConfiguration pluginConfig = plugin.getConfig();

        String host = pluginConfig.getString("database.mysql.host", "localhost");
        int port = pluginConfig.getInt("database.mysql.port", 3306);
        String database = pluginConfig.getString("database.mysql.database", "weguardian");
        String username = pluginConfig.getString("database.mysql.username", "root");
        String password = pluginConfig.getString("database.mysql.password", "");

        config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=false&serverTimezone=UTC");
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
    }

    private void setupSQLiteConfig(HikariConfig config) {
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "weguardian.db");
        config.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
        config.setDriverClassName("org.sqlite.JDBC");

        config.setMaximumPoolSize(1);
        config.setConnectionTimeout(30000);
    }


    private String getNowFunction() {
        return useMySQL ? "NOW()" : "CURRENT_TIMESTAMP";
    }

    @Override
    public CompletableFuture<Void> createTables() {
        return CompletableFuture.runAsync(() -> {
            try (Connection conn = dataSource.getConnection()) {
                createPlayersTable(conn);
                createPunishmentsTable(conn);
                createBanwaveTable(conn);
                createPlayerConnectionsTable(conn);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create tables", e);
            }
        });
    }

    private void createPlayersTable(Connection conn) throws SQLException {
        String sql = useMySQL ?
                "CREATE TABLE IF NOT EXISTS wg_players (" +
                        "uuid VARCHAR(36) PRIMARY KEY," +
                        "name VARCHAR(16) NOT NULL," +
                        "ip_address VARCHAR(45)," +
                        "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "banned BOOLEAN DEFAULT FALSE," +
                        "muted BOOLEAN DEFAULT FALSE," +
                        "INDEX idx_name (name)" +
                        ")" :
                "CREATE TABLE IF NOT EXISTS wg_players (" +
                        "uuid TEXT PRIMARY KEY," +
                        "name TEXT NOT NULL," +
                        "ip_address TEXT," +
                        "first_join TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "banned INTEGER DEFAULT 0," +
                        "muted INTEGER DEFAULT 0" +
                        ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void createPunishmentsTable(Connection conn) throws SQLException {
        String sql = useMySQL ?
                "CREATE TABLE IF NOT EXISTS wg_punishments (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "target_uuid VARCHAR(36) NOT NULL," +
                        "target_name VARCHAR(16) NOT NULL," +
                        "staff_uuid VARCHAR(36) NOT NULL," +
                        "staff_name VARCHAR(16) NOT NULL," +
                        "type VARCHAR(20) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "expires_at TIMESTAMP NULL," +
                        "active BOOLEAN DEFAULT TRUE," +
                        "removed_by_uuid VARCHAR(36) NULL," +
                        "removed_by_name VARCHAR(16) NULL," +
                        "removed_at TIMESTAMP NULL," +
                        "removal_reason TEXT NULL," +
                        "server_name VARCHAR(50) NOT NULL," +
                        "INDEX idx_target (target_uuid)," +
                        "INDEX idx_staff (staff_uuid)," +
                        "INDEX idx_type (type)," +
                        "INDEX idx_active (active)" +
                        ")" :
                "CREATE TABLE IF NOT EXISTS wg_punishments (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "target_uuid TEXT NOT NULL," +
                        "target_name TEXT NOT NULL," +
                        "staff_uuid TEXT NOT NULL," +
                        "staff_name TEXT NOT NULL," +
                        "type TEXT NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "expires_at TIMESTAMP NULL," +
                        "active INTEGER DEFAULT 1," +
                        "removed_by_uuid TEXT NULL," +
                        "removed_by_name TEXT NULL," +
                        "removed_at TIMESTAMP NULL," +
                        "removal_reason TEXT NULL," +
                        "server_name TEXT NOT NULL" +
                        ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void createBanwaveTable(Connection conn) throws SQLException {
        String sql = useMySQL ?
                "CREATE TABLE IF NOT EXISTS wg_banwave (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY," +
                        "target_uuid VARCHAR(36) NOT NULL," +
                        "target_name VARCHAR(16) NOT NULL," +
                        "staff_uuid VARCHAR(36) NOT NULL," +
                        "staff_name VARCHAR(16) NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "executed BOOLEAN DEFAULT FALSE," +
                        "executed_at TIMESTAMP NULL," +
                        "INDEX idx_executed (executed)" +
                        ")" :
                "CREATE TABLE IF NOT EXISTS wg_banwave (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "target_uuid TEXT NOT NULL," +
                        "target_name TEXT NOT NULL," +
                        "staff_uuid TEXT NOT NULL," +
                        "staff_name TEXT NOT NULL," +
                        "reason TEXT NOT NULL," +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "executed INTEGER DEFAULT 0," +
                        "executed_at TIMESTAMP NULL" +
                        ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    private void createPlayerConnectionsTable(Connection conn) throws SQLException {
        String sql = useMySQL ?
                "CREATE TABLE IF NOT EXISTS wg_player_connections (" +
                        "player_uuid VARCHAR(36) NOT NULL," +
                        "player_name VARCHAR(16) NOT NULL," +
                        "ip_address VARCHAR(45) NOT NULL," +
                        "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (player_uuid, ip_address)," +
                        "INDEX idx_player (player_uuid)," +
                        "INDEX idx_ip (ip_address)" +
                        ")" :
                "CREATE TABLE IF NOT EXISTS wg_player_connections (" +
                        "player_uuid TEXT NOT NULL," +
                        "player_name TEXT NOT NULL," +
                        "ip_address TEXT NOT NULL," +
                        "last_seen TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                        "PRIMARY KEY (player_uuid, ip_address)" +
                        ")";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        }
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        return CompletableFuture.runAsync(() -> {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        });
    }

    @Override
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM wg_players WHERE uuid = ?";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return mapPlayerData(rs);
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player data", e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<PlayerData> getPlayerData(String name) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM wg_players WHERE name = ? ORDER BY last_seen DESC LIMIT 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, name);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    return mapPlayerData(rs);
                }
                return null;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player data by name", e);
                return null;
            }
        });
    }

    private PlayerData mapPlayerData(ResultSet rs) throws SQLException {
        PlayerData data = new PlayerData();
        data.setUuid(UUID.fromString(rs.getString("uuid")));
        data.setName(rs.getString("name"));
        data.setIpAddress(rs.getString("ip_address"));

        Timestamp firstJoin = rs.getTimestamp("first_join");
        if (firstJoin != null) {
            data.setFirstJoin(firstJoin.toLocalDateTime());
        }

        Timestamp lastSeen = rs.getTimestamp("last_seen");
        if (lastSeen != null) {
            data.setLastSeen(lastSeen.toLocalDateTime());
        }

        data.setBanned(rs.getBoolean("banned"));
        data.setMuted(rs.getBoolean("muted"));

        return data;
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            String sql = useMySQL ?
                    "INSERT INTO wg_players (uuid, name, ip_address, first_join, last_seen, banned, muted) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                            "ON DUPLICATE KEY UPDATE name = VALUES(name), ip_address = VALUES(ip_address), " +
                            "last_seen = VALUES(last_seen), banned = VALUES(banned), muted = VALUES(muted)" :
                    "INSERT OR REPLACE INTO wg_players (uuid, name, ip_address, first_join, last_seen, banned, muted) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, playerData.getUuid().toString());
                stmt.setString(2, playerData.getName());
                stmt.setString(3, playerData.getIpAddress());
                stmt.setTimestamp(4, Timestamp.valueOf(playerData.getFirstJoin()));
                stmt.setTimestamp(5, Timestamp.valueOf(playerData.getLastSeen()));
                stmt.setBoolean(6, playerData.isBanned());
                stmt.setBoolean(7, playerData.isMuted());

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player data", e);
            }
        });
    }

    @Override
    public CompletableFuture<Integer> addPunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO wg_punishments (target_uuid, target_name, staff_uuid, staff_name, " +
                    "type, reason, created_at, expires_at, active, server_name) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, punishment.getTargetUuid().toString());
                stmt.setString(2, punishment.getTargetName());
                stmt.setString(3, punishment.getStaffUuid().toString());
                stmt.setString(4, punishment.getStaffName());
                stmt.setString(5, punishment.getType().name());
                stmt.setString(6, punishment.getReason());
                stmt.setTimestamp(7, Timestamp.valueOf(punishment.getCreatedAt()));

                if (punishment.getExpiresAt() != null) {
                    stmt.setTimestamp(8, Timestamp.valueOf(punishment.getExpiresAt()));
                } else {
                    stmt.setNull(8, Types.TIMESTAMP);
                }

                stmt.setBoolean(9, punishment.isActive());
                stmt.setString(10, punishment.getServerName());

                stmt.executeUpdate();

                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    return keys.getInt(1);
                }
                return -1;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to add punishment", e);
                return -1;
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM wg_punishments WHERE target_uuid = ? AND active = ? " +
                    "AND (expires_at IS NULL OR expires_at > ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setBoolean(2, true);
                stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));

                ResultSet rs = stmt.executeQuery();
                List<Punishment> punishments = new ArrayList<>();

                while (rs.next()) {
                    punishments.add(mapPunishment(rs));
                }

                return punishments;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active punishments", e);
                return new ArrayList<>();
            }
        });
    }

    private Punishment mapPunishment(ResultSet rs) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(rs.getInt("id"));
        punishment.setTargetUuid(UUID.fromString(rs.getString("target_uuid")));
        punishment.setTargetName(rs.getString("target_name"));
        punishment.setStaffUuid(UUID.fromString(rs.getString("staff_uuid")));
        punishment.setStaffName(rs.getString("staff_name"));
        punishment.setType(PunishmentType.valueOf(rs.getString("type")));
        punishment.setReason(rs.getString("reason"));
        punishment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());

        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            punishment.setExpiresAt(expiresAt.toLocalDateTime());
        }

        punishment.setActive(rs.getBoolean("active"));
        punishment.setServerName(rs.getString("server_name"));

        String removedByUuid = rs.getString("removed_by_uuid");
        if (removedByUuid != null) {
            punishment.setRemovedByUuid(UUID.fromString(removedByUuid));
            punishment.setRemovedByName(rs.getString("removed_by_name"));

            Timestamp removedAt = rs.getTimestamp("removed_at");
            if (removedAt != null) {
                punishment.setRemovedAt(removedAt.toLocalDateTime());
            }

            punishment.setRemovalReason(rs.getString("removal_reason"));
        }

        return punishment;
    }

    @Override
    public CompletableFuture<Integer> getStaffPunishmentCount(UUID staffUuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE staff_uuid = ? AND type = ? AND active = 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, staffUuid.toString());
                stmt.setString(2, type.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting staff punishment count: " + e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getTotalPunishmentCount(PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE type = ? AND active = 1";
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, type.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting total punishment count: " + e.getMessage());
                return 0;
            }
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getRecentPunishments(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments ORDER BY created_at DESC LIMIT ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting recent punishments: " + e.getMessage());
            }

            return punishments;
        });
    }

    @Override
    public CompletableFuture<Integer> addBanwaveEntry(BanwaveEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "INSERT INTO wg_banwave (target_uuid, target_name, staff_uuid, staff_name, reason, created_at, executed) VALUES (?, ?, ?, ?, ?, ?, ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, entry.getTargetUuid().toString());
                stmt.setString(2, entry.getTargetName());
                stmt.setString(3, entry.getStaffUuid().toString());
                stmt.setString(4, entry.getStaffName());
                stmt.setString(5, entry.getReason());
                stmt.setTimestamp(6, Timestamp.valueOf(entry.getCreatedAt()));
                stmt.setBoolean(7, entry.isExecuted());

                int affectedRows = stmt.executeUpdate();
                if (affectedRows > 0) {
                    try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                        if (generatedKeys.next()) {
                            return generatedKeys.getInt(1);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error adding banwave entry: " + e.getMessage());
            }

            return -1;
        });
    }

    @Override
    public CompletableFuture<List<BanwaveEntry>> getPendingBanwaveEntries() {
        return CompletableFuture.supplyAsync(() -> {
            List<BanwaveEntry> entries = new ArrayList<>();
            String sql = "SELECT * FROM wg_banwave WHERE executed = 0 ORDER BY created_at ASC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    entries.add(resultSetToBanwaveEntry(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting pending banwave entries: " + e.getMessage());
            }

            return entries;
        });
    }

    @Override
    public CompletableFuture<Void> executeBanwaveEntry(int id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE wg_banwave SET executed = 1 WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error executing banwave entry: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeBanwaveEntry(int id) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM wg_banwave WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing banwave entry: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerBanned(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE target_uuid = ? AND type IN ('BAN', 'TEMPBAN') AND active = 1 AND (expires_at IS NULL OR expires_at > ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setLong(2, System.currentTimeMillis());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking if player is banned: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerMuted(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE target_uuid = ? AND type IN ('MUTE', 'TEMPMUTE') AND active = 1 AND (expires_at IS NULL OR expires_at > ?)";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setLong(2, System.currentTimeMillis());

                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking if player is muted: " + e.getMessage());
                return false;
            }
        });
    }

    @Override
    public CompletableFuture<List<String>> searchPlayers(String query) {
        return CompletableFuture.supplyAsync(() -> {
            List<String> results = new ArrayList<>();
            String sql = "SELECT DISTINCT player_name FROM wg_punishments WHERE player_name LIKE ? LIMIT 10";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, "%" + query + "%");
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    results.add(rs.getString("player_name"));
                }

            } catch (SQLException e) {
                plugin.getLogger().severe("Failed to search players: " + e.getMessage());
            }

            return results;
        }, executorService);
    }

    @Override
    public CompletableFuture<Integer> getTotalPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            String query = "SELECT COUNT(*) FROM wg_punishments";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query);
                 ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {
                    return resultSet.getInt(1);
                }
                return 0;
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting total punishments: " + e.getMessage());
                return 0;
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<List<DatabaseManager.PlayerConnection>> getPlayerConnections(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.PlayerConnection> connections = new ArrayList<>();
            String query = "SELECT player_uuid, player_name, ip_address, last_seen FROM wg_player_connections WHERE player_uuid = ? ORDER BY last_seen DESC";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                statement.setString(1, uuid.toString());
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
                    String playerName = resultSet.getString("player_name");
                    String ipAddress = resultSet.getString("ip_address");
                    LocalDateTime timestamp = resultSet.getTimestamp("last_seen").toLocalDateTime();

                    connections.add(new PlayerConnectionImpl(playerUuid, playerName, ipAddress, timestamp));
                }

            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Error getting player connections for UUID: " + uuid, e);
            }

            return connections;
        }, executorService);
    }

    @Override
    public CompletableFuture<List<DatabaseManager.PlayerConnection>> getPlayersFromIP(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            List<DatabaseManager.PlayerConnection> players = new ArrayList<>();
            String query = "SELECT player_uuid, player_name, ip_address, last_seen FROM wg_player_connections WHERE ip_address = ? ORDER BY last_seen DESC";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                statement.setString(1, ip);

                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        UUID playerUuid = UUID.fromString(resultSet.getString("player_uuid"));
                        String playerName = resultSet.getString("player_name");
                        String ipAddress = resultSet.getString("ip_address");
                        LocalDateTime timestamp = resultSet.getTimestamp("last_seen").toLocalDateTime();

                        players.add(new PlayerConnectionImpl(playerUuid, playerName, ipAddress, timestamp));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting players from IP: " + e.getMessage());
            }

            return players;
        }, executorService);
    }

    @Override
    public CompletableFuture<Void> recordPlayerConnection(UUID uuid, String playerName, String ip) {
        return CompletableFuture.runAsync(() -> {
            String query = "INSERT INTO wg_player_connections (player_uuid, player_name, ip_address, last_seen) " +
                    "VALUES (?, ?, ?, " + getNowFunction() + ") " +
                    "ON DUPLICATE KEY UPDATE player_name = VALUES(player_name), last_seen = VALUES(last_seen)";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(query)) {

                statement.setString(1, uuid.toString());
                statement.setString(2, playerName);
                statement.setString(3, ip);

                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error recording player connection: " + e.getMessage());
            }
        }, executorService);
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllActivePunishments() {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments WHERE active = 1 AND (expires_at IS NULL OR expires_at > " + getNowFunction() + ") ORDER BY created_at DESC";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql);
                 ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {
                    punishments.add(mapResultSetToPunishment(resultSet));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting all active punishments: " + e.getMessage());
            }

            return punishments;
        }, executorService);
    }

    private Punishment mapResultSetToPunishment(ResultSet resultSet) throws SQLException {
        Punishment punishment = new Punishment();
        punishment.setId(resultSet.getInt("id"));
        punishment.setTargetUuid(UUID.fromString(resultSet.getString("target_uuid")));
        punishment.setTargetName(resultSet.getString("target_name"));
        punishment.setStaffUuid(UUID.fromString(resultSet.getString("staff_uuid")));
        punishment.setStaffName(resultSet.getString("staff_name"));
        punishment.setType(PunishmentType.valueOf(resultSet.getString("type")));
        punishment.setReason(resultSet.getString("reason"));
        punishment.setCreatedAt(resultSet.getTimestamp("created_at").toLocalDateTime());

        Timestamp expiresAt = resultSet.getTimestamp("expires_at");
        if (expiresAt != null) {
            punishment.setExpiresAt(expiresAt.toLocalDateTime());
        }

        punishment.setActive(resultSet.getBoolean("active"));
        punishment.setServerName(resultSet.getString("server_name"));

        String removedByUuid = resultSet.getString("removed_by_uuid");
        if (removedByUuid != null) {
            punishment.setRemovedByUuid(UUID.fromString(removedByUuid));
            punishment.setRemovedByName(resultSet.getString("removed_by_name"));

            Timestamp removedAt = resultSet.getTimestamp("removed_at");
            if (removedAt != null) {
                punishment.setRemovedAt(removedAt.toLocalDateTime());
            }

            punishment.setRemovalReason(resultSet.getString("removal_reason"));
        }

        return punishment;
    }

    @Override
    public CompletableFuture<Punishment> getPunishment(int id) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM wg_punishments WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, id);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapPunishment(rs);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting punishment: " + e.getMessage());
            }

            return null;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments WHERE target_uuid = ? ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting punishment history: " + e.getMessage());
            }

            return punishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments WHERE target_uuid = ? AND type = ? ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, type.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting punishment history by type: " + e.getMessage());
            }

            return punishments;
        });
    }

    @Override
    public CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT * FROM wg_punishments WHERE target_uuid = ? AND type = ? AND active = 1 AND (expires_at IS NULL OR expires_at > " + getNowFunction() + ") ORDER BY created_at DESC LIMIT 1";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, type.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return mapPunishment(rs);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting active punishment: " + e.getMessage());
            }

            return null;
        });
    }

    @Override
    public CompletableFuture<Void> removePunishment(int id, UUID removedBy, String removedByName, String reason) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE wg_punishments SET active = 0, removed_by_uuid = ?, removed_by_name = ?, removed_at = " + getNowFunction() + ", removal_reason = ? WHERE id = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, removedBy.toString());
                stmt.setString(2, removedByName);
                stmt.setString(3, reason);
                stmt.setInt(4, id);
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error removing punishment: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Void> expirePunishments() {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE wg_punishments SET active = 0 WHERE active = 1 AND expires_at IS NOT NULL AND expires_at <= " + getNowFunction();

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                int updated = stmt.executeUpdate();

                if (updated > 0) {
                    plugin.getLogger().info("Expired " + updated + " punishments");
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error expiring punishments: " + e.getMessage());
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isIPMuted(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE target_ip = ? AND type = 'IPMUTE' AND active = 1 AND (expires_at IS NULL OR expires_at > " + getNowFunction() + ")";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, ip);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking IP mute status for " + ip + ": " + e.getMessage());
            }

            return false;
        }, executorService);
    }

    @Override
    public CompletableFuture<Boolean> isIPBanned(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT COUNT(*) FROM wg_punishments WHERE target_ip = ? AND type IN ('IPBAN', 'IPTEMPBAN') AND active = 1 AND (expires_at IS NULL OR expires_at > " + getNowFunction() + ")";

            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                statement.setString(1, ip);

                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getInt(1) > 0;
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error checking IP ban status for " + ip + ": " + e.getMessage());
            }

            return false;
        }, executorService);
    }

    @Override
    public void close() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private BanwaveEntry resultSetToBanwaveEntry(ResultSet rs) throws SQLException {
        return new BanwaveEntry(
                rs.getInt("id"),
                UUID.fromString(rs.getString("target_uuid")),
                rs.getString("target_name"),
                UUID.fromString(rs.getString("staff_uuid")),
                rs.getString("staff_name"),
                rs.getString("reason"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("executed")
        );
    }

    @Override
    public CompletableFuture<Punishment> getPunishmentById(int id) {
        return getPunishment(id);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentsByType(UUID uuid, PunishmentType type) {
        return getPunishmentHistory(uuid, type);
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentsByStaff(String staffName) {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments WHERE staff_name = ? ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, staffName);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapPunishment(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting punishments by staff: " + e.getMessage());
            }

            return punishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            List<Punishment> punishments = new ArrayList<>();
            String sql = "SELECT * FROM wg_punishments ORDER BY created_at DESC";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    punishments.add(mapPunishment(rs));
                }
            } catch (SQLException e) {
                plugin.getLogger().severe("Error getting all punishments: " + e.getMessage());
            }

            return punishments;
        });
    }

    @Override
    public CompletableFuture<Void> updatePlayerName(UUID uuid, String newName) {
        return CompletableFuture.runAsync(() -> {
            String sql = "UPDATE wg_players SET name = ? WHERE uuid = ?";

            try (Connection conn = dataSource.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, newName);
                stmt.setString(2, uuid.toString());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().severe("Error updating player name: " + e.getMessage());
            }
        }, executorService);
    }

    private static class PlayerConnectionImpl implements DatabaseManager.PlayerConnection {
        private final UUID uuid;
        private final String playerName;
        private final String ip;
        private final LocalDateTime timestamp;

        public PlayerConnectionImpl(UUID uuid, String playerName, String ip, LocalDateTime timestamp) {
            this.uuid = uuid;
            this.playerName = playerName;
            this.ip = ip;
            this.timestamp = timestamp;
        }

        @Override
        public UUID getUuid() {
            return uuid;
        }

        @Override
        public String getPlayerName() {
            return playerName;
        }

        @Override
        public String getIp() {
            return ip;
        }

        @Override
        public LocalDateTime getTimestamp() {
            return timestamp;
        }
    }
}
