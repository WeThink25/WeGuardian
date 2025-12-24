package me.wethink.weguardian.database;

import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.model.PunishmentType;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PunishmentDAO {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;

    public PunishmentDAO(WeGuardian plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    public CompletableFuture<Integer> insert(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        INSERT INTO punishments
                        (target_uuid, target_name, target_ip, staff_uuid, staff_name, type, reason, created_at, expires_at, active)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """;

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                stmt.setString(1, punishment.getTargetUUID() != null ? punishment.getTargetUUID().toString() : null);
                stmt.setString(2, punishment.getTargetName());
                stmt.setString(3, punishment.getTargetIp());
                stmt.setString(4, punishment.getStaffUUID() != null ? punishment.getStaffUUID().toString() : null);
                stmt.setString(5, punishment.getStaffName());
                stmt.setString(6, punishment.getType().name());
                stmt.setString(7, punishment.getReason());
                stmt.setLong(8, punishment.getCreatedAt().toEpochMilli());
                stmt.setObject(9, punishment.getExpiresAt() != null ? punishment.getExpiresAt().toEpochMilli() : null);
                stmt.setInt(10, punishment.isActive() ? 1 : 0);

                stmt.executeUpdate();

                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
                return -1;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to insert punishment", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID targetUUID) {
        return getActivePunishment(targetUUID, PunishmentType.BAN, PunishmentType.TEMPBAN);
    }

    public CompletableFuture<Optional<Punishment>> getActiveMute(UUID targetUUID) {
        return getActivePunishment(targetUUID, PunishmentType.MUTE, PunishmentType.TEMPMUTE);
    }

    public CompletableFuture<Optional<Punishment>> getActiveIpBan(String ipAddress) {
        return getActiveIpPunishment(ipAddress, PunishmentType.BANIP, PunishmentType.TEMPBANIP);
    }

    public CompletableFuture<Optional<Punishment>> getActiveIpMute(String ipAddress) {
        return getActiveIpPunishment(ipAddress, PunishmentType.MUTEIP, PunishmentType.TEMPMUTEIP);
    }

    private CompletableFuture<Optional<Punishment>> getActiveIpPunishment(String ipAddress, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sql = new StringBuilder("""
                        SELECT * FROM punishments
                        WHERE target_ip = ? AND active = 1
                        AND (expires_at IS NULL OR expires_at > ?)
                        AND type IN (
                    """);

            for (int i = 0; i < types.length; i++) {
                sql.append("?");
                if (i < types.length - 1)
                    sql.append(", ");
            }
            sql.append(") ORDER BY created_at DESC LIMIT 1");

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                stmt.setString(1, ipAddress);
                stmt.setLong(2, Instant.now().toEpochMilli());
                for (int i = 0; i < types.length; i++) {
                    stmt.setString(3 + i, types[i].name());
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSet(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active IP punishment", e);
                throw new RuntimeException(e);
            }
        });
    }

    private CompletableFuture<Optional<Punishment>> getActivePunishment(UUID targetUUID, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sql = new StringBuilder("""
                        SELECT * FROM punishments
                        WHERE target_uuid = ? AND active = 1
                        AND (expires_at IS NULL OR expires_at > ?)
                        AND type IN (
                    """);

            for (int i = 0; i < types.length; i++) {
                sql.append("?");
                if (i < types.length - 1)
                    sql.append(", ");
            }
            sql.append(") ORDER BY created_at DESC LIMIT 1");

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                stmt.setString(1, targetUUID.toString());
                stmt.setLong(2, Instant.now().toEpochMilli());
                for (int i = 0; i < types.length; i++) {
                    stmt.setString(3 + i, types[i].name());
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(mapResultSet(rs));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active punishment", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<Punishment>> getHistory(UUID targetUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT * FROM punishments
                        WHERE target_uuid = ?
                        ORDER BY created_at DESC
                        LIMIT 50
                    """;

            List<Punishment> punishments = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, targetUUID.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get punishment history", e);
                throw new RuntimeException(e);
            }
            return punishments;
        });
    }

    public CompletableFuture<List<Punishment>> getRecentPunishments(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT * FROM punishments
                        ORDER BY created_at DESC
                        LIMIT ?
                    """;

            List<Punishment> punishments = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setInt(1, limit);

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get recent punishments", e);
                throw new RuntimeException(e);
            }
            return punishments;
        });
    }

    public CompletableFuture<Integer> deactivate(UUID targetUUID, UUID removedByUUID, String removedByName,
            String reason, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sql = new StringBuilder("""
                        UPDATE punishments
                        SET active = 0, removed_by_uuid = ?, removed_by_name = ?, removed_at = ?, remove_reason = ?
                        WHERE target_uuid = ? AND active = 1 AND type IN (
                    """);

            for (int i = 0; i < types.length; i++) {
                sql.append("?");
                if (i < types.length - 1)
                    sql.append(", ");
            }
            sql.append(")");

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                stmt.setString(1, removedByUUID != null ? removedByUUID.toString() : null);
                stmt.setString(2, removedByName);
                stmt.setLong(3, Instant.now().toEpochMilli());
                stmt.setString(4, reason);
                stmt.setString(5, targetUUID.toString());

                for (int i = 0; i < types.length; i++) {
                    stmt.setString(6 + i, types[i].name());
                }

                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deactivate punishment", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Integer> deactivateIp(String ipAddress, UUID removedByUUID, String removedByName,
            String reason, PunishmentType... types) {
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder sql = new StringBuilder("""
                        UPDATE punishments
                        SET active = 0, removed_by_uuid = ?, removed_by_name = ?, removed_at = ?, remove_reason = ?
                        WHERE target_ip = ? AND active = 1 AND type IN (
                    """);

            for (int i = 0; i < types.length; i++) {
                sql.append("?");
                if (i < types.length - 1)
                    sql.append(", ");
            }
            sql.append(")");

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

                stmt.setString(1, removedByUUID != null ? removedByUUID.toString() : null);
                stmt.setString(2, removedByName);
                stmt.setLong(3, Instant.now().toEpochMilli());
                stmt.setString(4, reason);
                stmt.setString(5, ipAddress);

                for (int i = 0; i < types.length; i++) {
                    stmt.setString(6 + i, types[i].name());
                }

                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to deactivate IP punishment", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<List<Punishment>> getExpiredPunishments() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT * FROM punishments
                        WHERE active = 1 AND expires_at IS NOT NULL AND expires_at <= ?
                    """;

            List<Punishment> punishments = new ArrayList<>();
            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setLong(1, Instant.now().toEpochMilli());

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        punishments.add(mapResultSet(rs));
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get expired punishments", e);
                throw new RuntimeException(e);
            }
            return punishments;
        });
    }

    public CompletableFuture<Integer> cleanupExpired() {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        UPDATE punishments
                        SET active = 0, removed_by_name = 'EXPIRED', removed_at = ?
                        WHERE active = 1 AND expires_at IS NOT NULL AND expires_at <= ?
                    """;

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                long now = Instant.now().toEpochMilli();
                stmt.setLong(1, now);
                stmt.setLong(2, now);

                return stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to cleanup expired punishments", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Void> savePlayerIp(UUID uuid, String name, String ipAddress) {
        return CompletableFuture.runAsync(() -> {
            String sql = """
                        INSERT OR REPLACE INTO player_ips (uuid, name, ip_address, last_seen)
                        VALUES (?, ?, ?, ?)
                    """;

            if (databaseManager.getDatabaseType().equals("mysql")) {
                sql = """
                            INSERT INTO player_ips (uuid, name, ip_address, last_seen)
                            VALUES (?, ?, ?, ?)
                            ON DUPLICATE KEY UPDATE name = VALUES(name), ip_address = VALUES(ip_address), last_seen = VALUES(last_seen)
                        """;
            }

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());
                stmt.setString(2, name);
                stmt.setString(3, ipAddress);
                stmt.setLong(4, Instant.now().toEpochMilli());

                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player IP", e);
            }
        });
    }

    public CompletableFuture<Optional<String>> getPlayerIp(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT ip_address FROM player_ips WHERE uuid = ?
                    """;

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, uuid.toString());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getString("ip_address"));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get player IP", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<String>> getActiveIpBanByUUID(UUID targetUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT target_ip FROM punishments
                        WHERE target_uuid = ? AND active = 1
                        AND target_ip IS NOT NULL
                        AND (expires_at IS NULL OR expires_at > ?)
                        AND type IN (?, ?)
                        ORDER BY created_at DESC LIMIT 1
                    """;

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, targetUUID.toString());
                stmt.setLong(2, Instant.now().toEpochMilli());
                stmt.setString(3, PunishmentType.BANIP.name());
                stmt.setString(4, PunishmentType.TEMPBANIP.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString("target_ip"));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active IP ban by UUID", e);
                throw new RuntimeException(e);
            }
        });
    }

    public CompletableFuture<Optional<String>> getActiveIpMuteByUUID(UUID targetUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = """
                        SELECT target_ip FROM punishments
                        WHERE target_uuid = ? AND active = 1
                        AND target_ip IS NOT NULL
                        AND (expires_at IS NULL OR expires_at > ?)
                        AND type IN (?, ?)
                        ORDER BY created_at DESC LIMIT 1
                    """;

            try (Connection conn = databaseManager.getConnection();
                    PreparedStatement stmt = conn.prepareStatement(sql)) {

                stmt.setString(1, targetUUID.toString());
                stmt.setLong(2, Instant.now().toEpochMilli());
                stmt.setString(3, PunishmentType.MUTEIP.name());
                stmt.setString(4, PunishmentType.TEMPMUTEIP.name());

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        return Optional.ofNullable(rs.getString("target_ip"));
                    }
                }
                return Optional.empty();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get active IP mute by UUID", e);
                throw new RuntimeException(e);
            }
        });
    }

    private Punishment mapResultSet(ResultSet rs) throws SQLException {
        Punishment p = new Punishment();
        p.setId(rs.getInt("id"));

        String targetUuidStr = rs.getString("target_uuid");
        p.setTargetUUID(targetUuidStr != null ? UUID.fromString(targetUuidStr) : null);
        p.setTargetName(rs.getString("target_name"));
        p.setTargetIp(rs.getString("target_ip"));

        String staffUuidStr = rs.getString("staff_uuid");
        p.setStaffUUID(staffUuidStr != null ? UUID.fromString(staffUuidStr) : null);
        p.setStaffName(rs.getString("staff_name"));

        p.setType(PunishmentType.valueOf(rs.getString("type")));
        p.setReason(rs.getString("reason"));
        p.setCreatedAt(Instant.ofEpochMilli(rs.getLong("created_at")));

        long expiresAt = rs.getLong("expires_at");
        p.setExpiresAt(rs.wasNull() ? null : Instant.ofEpochMilli(expiresAt));

        p.setActive(rs.getInt("active") == 1);

        String removedByUuidStr = rs.getString("removed_by_uuid");
        p.setRemovedByUUID(removedByUuidStr != null ? UUID.fromString(removedByUuidStr) : null);
        p.setRemovedByName(rs.getString("removed_by_name"));

        long removedAt = rs.getLong("removed_at");
        p.setRemovedAt(rs.wasNull() ? null : Instant.ofEpochMilli(removedAt));

        p.setRemoveReason(rs.getString("remove_reason"));

        return p;
    }
}
