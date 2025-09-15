package me.wethink.weGuardian.database;

import me.wethink.weGuardian.models.BanwaveEntry;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface DatabaseManager {

    CompletableFuture<Void> initialize();

    CompletableFuture<Boolean> connect();

    CompletableFuture<Void> disconnect();

    CompletableFuture<Void> createTables();

    void close();

    CompletableFuture<PlayerData> getPlayerData(UUID uuid);

    CompletableFuture<PlayerData> getPlayerData(String name);

    CompletableFuture<Void> savePlayerData(PlayerData playerData);

    CompletableFuture<Void> updatePlayerName(UUID uuid, String newName);

    CompletableFuture<Integer> addPunishment(Punishment punishment);

    CompletableFuture<Punishment> getPunishment(int id);

    CompletableFuture<Punishment> getPunishmentById(int id);

    CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid);

    CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid);

    CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid, PunishmentType type);

    CompletableFuture<List<Punishment>> getPunishmentsByType(UUID uuid, PunishmentType type);

    CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType type);

    CompletableFuture<Void> removePunishment(int id, UUID removedBy, String removedByName, String reason);

    CompletableFuture<Void> expirePunishments();

    CompletableFuture<Integer> getStaffPunishmentCount(UUID staffUuid, PunishmentType type);

    CompletableFuture<Integer> getTotalPunishmentCount(PunishmentType type);

    CompletableFuture<List<Punishment>> getRecentPunishments(int limit);

    CompletableFuture<List<Punishment>> getPunishmentsByStaff(String staffName);

    CompletableFuture<List<Punishment>> getAllPunishments();

    CompletableFuture<Integer> addBanwaveEntry(BanwaveEntry entry);

    CompletableFuture<List<BanwaveEntry>> getPendingBanwaveEntries();

    CompletableFuture<Void> executeBanwaveEntry(int id);

    CompletableFuture<Void> removeBanwaveEntry(int id);

    CompletableFuture<Boolean> isPlayerBanned(UUID uuid);

    CompletableFuture<Boolean> isPlayerMuted(UUID uuid);

    CompletableFuture<Boolean> isIPMuted(String ip);

    CompletableFuture<Boolean> isIPBanned(String ip);

    CompletableFuture<List<String>> searchPlayers(String query);

    CompletableFuture<Integer> getTotalPunishments();

    CompletableFuture<List<PlayerConnection>> getPlayerConnections(UUID uuid);

    CompletableFuture<List<PlayerConnection>> getPlayersFromIP(String ip);

    CompletableFuture<Void> recordPlayerConnection(UUID uuid, String playerName, String ip);

    CompletableFuture<List<Punishment>> getAllActivePunishments();

    interface PlayerConnection {
        UUID getUuid();

        String getPlayerName();

        String getIp();

        LocalDateTime getTimestamp();
    }
}
