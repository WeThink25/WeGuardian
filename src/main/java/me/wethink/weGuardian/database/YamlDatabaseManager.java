package me.wethink.weGuardian.database;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.BanwaveEntry;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class YamlDatabaseManager implements DatabaseManager {

    private final WeGuardian plugin;
    private final File dataDir;
    private final File globalFile;
    private final YamlConfiguration globalConfig;
    private final AtomicInteger nextId;
    private final Map<Integer, Punishment> punishmentCache;
    private final Map<UUID, PlayerData> playerDataCache;
    private final Map<Integer, BanwaveEntry> banwaveCache;
    private final Map<UUID, Map<String, Long>> playerConnectionsCache;
    private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    public YamlDatabaseManager(WeGuardian plugin) {
        this.plugin = plugin;
        this.dataDir = new File(plugin.getDataFolder(), "data");
        this.globalFile = new File(plugin.getDataFolder(), "global.yml");
        this.globalConfig = YamlConfiguration.loadConfiguration(globalFile);
        this.punishmentCache = new ConcurrentHashMap<>();
        this.playerDataCache = new ConcurrentHashMap<>();
        this.banwaveCache = new ConcurrentHashMap<>();
        this.playerConnectionsCache = new ConcurrentHashMap<>();
        this.nextId = new AtomicInteger(1);
        
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        
        loadData();
        setupAutoBackup();
    }

    private void loadData() {
        if (!globalFile.exists()) {
            try {
                globalFile.createNewFile();
                globalConfig.set("next_id", 1);
                globalConfig.set("banwave", new HashMap<>());
                saveGlobalConfig();
            } catch (IOException e) {
                plugin.getLogger().severe("Failed to create global YAML file: " + e.getMessage());
            }
        }

        nextId.set(globalConfig.getInt("next_id", 1));
        
        ConfigurationSection banwaveSection = globalConfig.getConfigurationSection("banwave");
        if (banwaveSection != null) {
            for (String key : banwaveSection.getKeys(false)) {
                try {
                    int id = Integer.parseInt(key);
                    ConfigurationSection entrySection = banwaveSection.getConfigurationSection(key);
                    if (entrySection != null) {
                        BanwaveEntry entry = deserializeBanwaveEntry(entrySection);
                        if (entry != null) {
                            banwaveCache.put(id, entry);
                        }
                    }
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("Invalid banwave ID in global YAML: " + key);
                }
            }
        }
        
        File[] playerFiles = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (playerFiles != null) {
            for (File playerFile : playerFiles) {
                try {
                    String fileName = playerFile.getName();
                    String uuidString = fileName.substring(0, fileName.length() - 4);
                    UUID playerUuid = UUID.fromString(uuidString);
                    
                    YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    
                    PlayerData playerData = deserializePlayerData(playerConfig, playerUuid);
                    if (playerData != null) {
                        playerDataCache.put(playerUuid, playerData);
                    }
                    ConfigurationSection connectionsSection = playerConfig.getConfigurationSection("connections");
                    if (connectionsSection != null) {
                        Map<String, Long> ipMap = new ConcurrentHashMap<>();
                        for (String ip : connectionsSection.getKeys(false)) {
                            long lastSeenTs = connectionsSection.getLong(ip, 0L);
                            ipMap.put(ip, lastSeenTs);
                        }
                        playerConnectionsCache.put(playerUuid, ipMap);
                    }
                    
                    ConfigurationSection punishmentsSection = playerConfig.getConfigurationSection("punishments");
                    if (punishmentsSection != null) {
                        for (String key : punishmentsSection.getKeys(false)) {
                            try {
                                int id = Integer.parseInt(key);
                                Punishment punishment = deserializePunishment(punishmentsSection.getConfigurationSection(key));
                                if (punishment != null) {
                                    punishmentCache.put(id, punishment);
                                }
                            } catch (NumberFormatException e) {
                                plugin.getLogger().warning("Invalid punishment ID in player file " + fileName + ": " + key);
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid UUID in filename: " + playerFile.getName());
                }
            }
        }
        
        File ipPunishmentsFile = new File(plugin.getDataFolder(), "ip_punishments.yml");
        if (ipPunishmentsFile.exists()) {
            YamlConfiguration ipConfig = YamlConfiguration.loadConfiguration(ipPunishmentsFile);
            ConfigurationSection punishmentsSection = ipConfig.getConfigurationSection("punishments");
            if (punishmentsSection != null) {
                for (String key : punishmentsSection.getKeys(false)) {
                    try {
                        int id = Integer.parseInt(key);
                        Punishment punishment = deserializePunishment(punishmentsSection.getConfigurationSection(key));
                        if (punishment != null) {
                            punishmentCache.put(id, punishment);
                        }
                    } catch (NumberFormatException e) {
                        plugin.getLogger().warning("Invalid punishment ID in IP punishments file: " + key);
                    }
                }
            }
        }
    }

    private File getPlayerFile(UUID uuid) {
        return new File(dataDir, uuid.toString() + ".yml");
    }

    private YamlConfiguration getPlayerConfig(UUID uuid) {
        File playerFile = getPlayerFile(uuid);
        return YamlConfiguration.loadConfiguration(playerFile);
    }

    private void savePlayerConfig(UUID uuid, YamlConfiguration config) {
        try {
            File playerFile = getPlayerFile(uuid);
            config.save(playerFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player YAML file for " + uuid + ": " + e.getMessage());
        }
    }

    private void saveGlobalConfig() {
        try {
            globalConfig.save(globalFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save global YAML file: " + e.getMessage());
        }
    }

    private void setupAutoBackup() {
        if (plugin.getConfig().getBoolean("database.yaml.auto_backup", true)) {
            int interval = plugin.getConfig().getInt("database.yaml.backup_interval", 300) * 20;
            plugin.getFoliaLib().getScheduler().runTimerAsync(this::createBackup, interval, interval);
        }
    }

    private void createBackup() {
        try {
            File backupDir = new File(plugin.getDataFolder(), "backups");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            File backupDataDir = new File(backupDir, "data_" + timestamp);
            backupDataDir.mkdirs();
            
            YamlConfiguration backupGlobal = new YamlConfiguration();
            if (globalConfig.getDefaults() != null) {
                backupGlobal.setDefaults(globalConfig.getDefaults());
            }
            for (String key : globalConfig.getKeys(true)) {
                backupGlobal.set(key, globalConfig.get(key));
            }
            backupGlobal.save(new File(backupDataDir, "global.yml"));
            
            File[] playerFiles = dataDir.listFiles((dir, name) -> name.endsWith(".yml"));
            if (playerFiles != null) {
                for (File playerFile : playerFiles) {
                    YamlConfiguration playerConfig = YamlConfiguration.loadConfiguration(playerFile);
                    YamlConfiguration backupPlayer = new YamlConfiguration();
                    if (playerConfig.getDefaults() != null) {
                        backupPlayer.setDefaults(playerConfig.getDefaults());
                    }
                    for (String key : playerConfig.getKeys(true)) {
                        backupPlayer.set(key, playerConfig.get(key));
                    }
                    backupPlayer.save(new File(backupDataDir, playerFile.getName()));
                }
            }
            
            File ipPunishmentsFile = new File(plugin.getDataFolder(), "ip_punishments.yml");
            if (ipPunishmentsFile.exists()) {
                YamlConfiguration ipConfig = YamlConfiguration.loadConfiguration(ipPunishmentsFile);
                YamlConfiguration backupIp = new YamlConfiguration();
                if (ipConfig.getDefaults() != null) {
                    backupIp.setDefaults(ipConfig.getDefaults());
                }
                for (String key : ipConfig.getKeys(true)) {
                    backupIp.set(key, ipConfig.get(key));
                }
                backupIp.save(new File(backupDataDir, "ip_punishments.yml"));
            }

            cleanOldBackups(backupDir);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to create backup: " + e.getMessage());
        }
    }

    private void cleanOldBackups(File backupDir) {
        File[] backups = backupDir.listFiles((dir, name) -> name.startsWith("data_"));
        if (backups != null && backups.length > plugin.getConfig().getInt("database.yaml.max_backups", 5)) {
            Arrays.sort(backups, Comparator.comparingLong(File::lastModified));
            for (int i = 0; i < backups.length - plugin.getConfig().getInt("database.yaml.max_backups", 5); i++) {
                deleteDirectory(backups[i]);
            }
        }
    }

    private void deleteDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        directory.delete();
    }

    @Override
    public CompletableFuture<Integer> addPunishment(Punishment punishment) {
        return CompletableFuture.supplyAsync(() -> {
            int id = nextId.getAndIncrement();
            punishment.setId(id);
            punishmentCache.put(id, punishment);
            plugin.debug("Added punishment to cache with ID: %d, type: %s, target: %s", 
                    id, punishment.getType(), punishment.getTargetName());
            
            UUID targetUuid = punishment.getTargetUuid();
            if (targetUuid != null) {
                plugin.debug("Saving punishment to player file: %s", targetUuid);
                YamlConfiguration playerConfig = getPlayerConfig(targetUuid);
                
                ConfigurationSection punishmentsSection = playerConfig.getConfigurationSection("punishments");
                if (punishmentsSection == null) {
                    punishmentsSection = playerConfig.createSection("punishments");
                    plugin.debug("Created punishments section for player: %s", targetUuid);
                }
                
                ConfigurationSection punishmentSection = punishmentsSection.createSection(String.valueOf(id));
                serializePunishment(punishmentSection, punishment);
                plugin.debug("Serialized punishment to player config: %s", targetUuid);
                
                savePlayerConfig(targetUuid, playerConfig);
            } else {
                plugin.debug("Saving IP punishment to IP punishments file");
                saveIPPunishment(punishment);
            }
            
            globalConfig.set("next_id", nextId.get());
            saveGlobalConfig();
            plugin.debug("Punishment saved successfully with ID: %d", id);
            
            return id;
        });
    }

    private void saveIPPunishment(Punishment punishment) {
        File ipPunishmentsFile = new File(plugin.getDataFolder(), "ip_punishments.yml");
        YamlConfiguration ipConfig = YamlConfiguration.loadConfiguration(ipPunishmentsFile);
        
        ConfigurationSection punishmentsSection = ipConfig.getConfigurationSection("punishments");
        if (punishmentsSection == null) {
            punishmentsSection = ipConfig.createSection("punishments");
        }
        
        ConfigurationSection punishmentSection = punishmentsSection.createSection(String.valueOf(punishment.getId()));
        serializePunishment(punishmentSection, punishment);
        
        try {
            ipConfig.save(ipPunishmentsFile);
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to save IP punishment: " + e.getMessage());
        }
    }

    @Override
    public CompletableFuture<Void> savePlayerData(PlayerData playerData) {
        return CompletableFuture.runAsync(() -> {
            playerDataCache.put(playerData.getUuid(), playerData);
            
            YamlConfiguration playerConfig = getPlayerConfig(playerData.getUuid());
            serializePlayerData(playerConfig, playerData);
            serializePlayerConnections(playerConfig, playerData.getUuid());
            savePlayerConfig(playerData.getUuid(), playerConfig);
        });
    }

    @Override
    public CompletableFuture<Void> removePunishment(int id, UUID removedBy, String removedByName, String reason) {
        return CompletableFuture.runAsync(() -> {
            Punishment punishment = punishmentCache.get(id);
            if (punishment != null) {
                punishment.setActive(false);
                punishment.setRemovedAt(LocalDateTime.now());
                punishment.setRemovedBy(removedByName);
                punishment.setRemovalReason(reason);
                punishment.setRemovedByUuid(removedBy);
                
                UUID targetUuid = punishment.getTargetUuid();
                if (targetUuid != null) {
                    YamlConfiguration playerConfig = getPlayerConfig(targetUuid);
                    ConfigurationSection punishmentSection = playerConfig.getConfigurationSection("punishments." + id);
                    if (punishmentSection != null) {
                        serializePunishment(punishmentSection, punishment);
                        savePlayerConfig(targetUuid, playerConfig);
                    }
                } else {
                    saveIPPunishment(punishment);
                }
            }
        });
    }

    private PlayerData deserializePlayerData(YamlConfiguration config, UUID uuid) {
        try {
            PlayerData playerData = new PlayerData();
            playerData.setUuid(uuid);
            playerData.setPlayerName(config.getString("player_name", "Unknown"));
            
            Object firstJoinRaw = config.get("first_join");
            if (firstJoinRaw instanceof String) {
                try {
                    LocalDateTime firstJoin = LocalDateTime.parse((String) firstJoinRaw, formatter);
                    playerData.setFirstJoin(firstJoin.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
                } catch (Exception e) {
                    playerData.setFirstJoin(System.currentTimeMillis());
                    plugin.getLogger().warning("Failed to parse first_join for " + uuid + ", using current time");
                }
            } else if (firstJoinRaw instanceof Number) {
                playerData.setFirstJoin(((Number) firstJoinRaw).longValue());
            } else {
                playerData.setFirstJoin(System.currentTimeMillis());
                plugin.getLogger().info("Migrated corrupted first_join data for " + uuid);
                config.set("first_join", System.currentTimeMillis());
            }
            
            Object lastJoinRaw = config.get("last_join");
            if (lastJoinRaw instanceof String) {
                try {
                    LocalDateTime lastJoin = LocalDateTime.parse((String) lastJoinRaw, formatter);
                    playerData.setLastJoin(lastJoin.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000);
                } catch (Exception e) {
                    playerData.setLastJoin(System.currentTimeMillis());
                    plugin.getLogger().warning("Failed to parse last_join for " + uuid + ", using current time");
                }
            } else if (lastJoinRaw instanceof Number) {
                playerData.setLastJoin(((Number) lastJoinRaw).longValue());
            } else {
                playerData.setLastJoin(System.currentTimeMillis());
                plugin.getLogger().info("Migrated corrupted last_join data for " + uuid);
                config.set("last_join", System.currentTimeMillis());
            }
            
            playerData.setLastIP(config.getString("last_ip", ""));
            return playerData;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize player data for " + uuid + ": " + e.getMessage());
            return null;
        }
    }

    private void serializePlayerData(YamlConfiguration config, PlayerData playerData) {
        config.set("player_name", playerData.getPlayerName());
        config.set("first_join", playerData.getFirstJoinTimestamp());
        config.set("last_join", playerData.getLastJoinTimestamp());
        config.set("last_ip", playerData.getLastIP());
    }

    private void serializePlayerConnections(YamlConfiguration config, UUID uuid) {
        Map<String, Long> ipMap = playerConnectionsCache.get(uuid);
        if (ipMap == null) return;
        ConfigurationSection section = config.getConfigurationSection("connections");
        if (section == null) {
            section = config.createSection("connections");
        }
        for (Map.Entry<String, Long> entry : ipMap.entrySet()) {
            section.set(entry.getKey(), entry.getValue());
        }
    }

    private BanwaveEntry deserializeBanwaveEntry(ConfigurationSection section) {
        try {
            BanwaveEntry entry = new BanwaveEntry();
            entry.setId(section.getInt("id"));
            entry.setPlayerName(section.getString("player_name"));
            entry.setReason(section.getString("reason"));
            entry.setStaffName(section.getString("staff_name"));
            entry.setExecuted(section.getBoolean("executed", false));
            return entry;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize banwave entry: " + e.getMessage());
            return null;
        }
    }

    private void serializePunishment(ConfigurationSection section, Punishment punishment) {
        section.set("id", punishment.getId());
        section.set("type", punishment.getType().name());
        section.set("target_uuid", punishment.getTargetUuid().toString());
        section.set("target_name", punishment.getTargetName());
        section.set("target_ip", punishment.getTargetIP());
        section.set("staff_uuid", punishment.getStaffUuid().toString());
        section.set("staff_name", punishment.getStaffName());
        section.set("reason", punishment.getReason());
        section.set("server_name", punishment.getServerName());
        section.set("created_at", punishment.getCreatedAt().format(formatter));
        section.set("active", punishment.isActive());
        
        if (punishment.getExpiresAt() != null) {
            section.set("expires_at", punishment.getExpiresAt().format(formatter));
        }
        
        if (punishment.getRemovedAt() != null) {
            section.set("removed_at", punishment.getRemovedAt().format(formatter));
            section.set("removed_by", punishment.getRemovedBy());
            section.set("removal_reason", punishment.getRemovalReason());
        }
    }

    private Punishment deserializePunishment(ConfigurationSection section) {
        if (section == null) return null;
        
        try {
            Punishment punishment = new Punishment();
            punishment.setId(section.getInt("id"));
            punishment.setType(PunishmentType.valueOf(section.getString("type")));
            punishment.setTargetUuid(UUID.fromString(section.getString("target_uuid")));
            punishment.setTargetName(section.getString("target_name"));
            punishment.setTargetIP(section.getString("target_ip"));
            punishment.setStaffUuid(UUID.fromString(section.getString("staff_uuid")));
            punishment.setStaffName(section.getString("staff_name"));
            punishment.setReason(section.getString("reason"));
            punishment.setServerName(section.getString("server_name"));
            punishment.setCreatedAt(LocalDateTime.parse(section.getString("created_at"), formatter));
            punishment.setActive(section.getBoolean("active", true));
            
            String expiresAt = section.getString("expires_at");
            if (expiresAt != null) {
                punishment.setExpiresAt(LocalDateTime.parse(expiresAt, formatter));
            }
            
            String removedAt = section.getString("removed_at");
            if (removedAt != null) {
                punishment.setRemovedAt(LocalDateTime.parse(removedAt, formatter));
                punishment.setRemovedBy(section.getString("removed_by"));
                punishment.setRemovalReason(section.getString("removal_reason"));
            }
            
            return punishment;
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to deserialize punishment: " + e.getMessage());
            return null;
        }
    }

    @Override
    public CompletableFuture<Integer> addBanwaveEntry(BanwaveEntry entry) {
        return CompletableFuture.supplyAsync(() -> {
            int id = nextId.getAndIncrement();
            entry.setId(id);
            banwaveCache.put(id, entry);
            
            ConfigurationSection banwaveSection = globalConfig.getConfigurationSection("banwave");
            if (banwaveSection == null) {
                banwaveSection = globalConfig.createSection("banwave");
            }
            
            ConfigurationSection entrySection = banwaveSection.createSection(String.valueOf(id));
            serializeBanwaveEntry(entrySection, entry);
            
            saveGlobalConfig();
            return id;
        });
    }

    private void serializeBanwaveEntry(ConfigurationSection section, BanwaveEntry entry) {
        section.set("id", entry.getId());
        section.set("player_name", entry.getPlayerName());
        section.set("reason", entry.getReason());
        section.set("staff_name", entry.getStaffName());
        section.set("executed", entry.isExecuted());
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Boolean> connect() {
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public CompletableFuture<Void> disconnect() {
        saveGlobalConfig();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> createTables() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void close() {
        saveGlobalConfig();
    }

    @Override
    public CompletableFuture<PlayerData> getPlayerData(UUID uuid) {
        return CompletableFuture.completedFuture(playerDataCache.get(uuid));
    }

    @Override
    public CompletableFuture<PlayerData> getPlayerData(String name) {
        return CompletableFuture.supplyAsync(() -> 
            playerDataCache.values().stream()
                .filter(p -> name.equalsIgnoreCase(p.getPlayerName()))
                .findFirst()
                .orElse(null)
        );
    }

    @Override
    public CompletableFuture<Void> updatePlayerName(UUID uuid, String newName) {
        return CompletableFuture.runAsync(() -> {
            PlayerData data = playerDataCache.get(uuid);
            if (data != null) {
                data.setPlayerName(newName);
                savePlayerConfig(uuid, getPlayerConfig(uuid));
            }
        });
    }

    @Override
    public CompletableFuture<Punishment> getPunishment(int id) {
        return CompletableFuture.completedFuture(punishmentCache.get(id));
    }

    @Override
    public CompletableFuture<Punishment> getPunishmentById(int id) {
        return getPunishment(id);
    }

    @Override
    public CompletableFuture<List<Punishment>> getActivePunishments(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            if (uuid == null) {
                return new ArrayList<>();
            }
            
            List<Punishment> activePunishments = new ArrayList<>();
            YamlConfiguration playerConfig = getPlayerConfig(uuid);
            
            ConfigurationSection punishmentsSection = playerConfig.getConfigurationSection("punishments");
            if (punishmentsSection != null) {
                for (String key : punishmentsSection.getKeys(false)) {
                    ConfigurationSection punishmentSection = punishmentsSection.getConfigurationSection(key);
                    if (punishmentSection != null) {
                        Punishment punishment = deserializePunishment(punishmentSection);
                        if (punishment != null && punishment.isActive() && !punishment.isExpired()) {
                            activePunishments.add(punishment);
                        }
                    }
                }
            }
            
            return activePunishments;
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .filter(p -> p.getTargetUuid() != null && p.getTargetUuid().equals(uuid))
                .sorted(Comparator.comparing(Punishment::getCreatedAt).reversed())
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID uuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .filter(p -> p.getTargetUuid().equals(uuid) && p.getType() == type)
                .sorted(Comparator.comparing(Punishment::getCreatedAt).reversed())
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentsByType(UUID uuid, PunishmentType type) {
        return getPunishmentHistory(uuid, type);
    }

    @Override
    public CompletableFuture<Punishment> getActivePunishment(UUID uuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .filter(p -> p.getTargetUuid().equals(uuid) && 
                           p.getType() == type && 
                           p.getRemovedAt() == null && 
                           (p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now())))
                .findFirst()
                .orElse(null)
        );
    }

    @Override
    public CompletableFuture<Void> expirePunishments() {
        return CompletableFuture.runAsync(() -> {
            LocalDateTime now = LocalDateTime.now();
            boolean changed = false;
            
            for (Punishment punishment : punishmentCache.values()) {
                if (punishment.getExpiresAt() != null && 
                    punishment.getExpiresAt().isBefore(now) && 
                    punishment.getRemovedAt() == null) {
                    
                    punishment.setRemovedAt(now);
                    punishment.setRemovedBy("System");
                    punishment.setRemovalReason("Expired");
                    
                    ConfigurationSection section = getPlayerConfig(punishment.getTargetUuid()).getConfigurationSection("punishments." + punishment.getId());
                    if (section != null) {
                        serializePunishment(section, punishment);
                        changed = true;
                    }
                }
            }
            
            if (changed) {
                for (Punishment punishment : punishmentCache.values()) {
                    if (punishment.getRemovedAt() != null) {
                        savePlayerConfig(punishment.getTargetUuid(), getPlayerConfig(punishment.getTargetUuid()));
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<Integer> getStaffPunishmentCount(UUID staffUuid, PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> 
            (int) punishmentCache.values().stream()
                .filter(p -> p.getStaffUuid().equals(staffUuid) && p.getType() == type)
                .count()
        );
    }

    @Override
    public CompletableFuture<Integer> getTotalPunishmentCount(PunishmentType type) {
        return CompletableFuture.supplyAsync(() -> 
            (int) punishmentCache.values().stream()
                .filter(p -> p.getType() == type)
                .count()
        );
    }

    @Override
    public CompletableFuture<List<Punishment>> getRecentPunishments(int limit) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .sorted(Comparator.comparing(Punishment::getCreatedAt).reversed())
                .limit(limit)
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<List<Punishment>> getPunishmentsByStaff(String staffName) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .filter(p -> staffName.equals(p.getStaffName()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllPunishments() {
        return CompletableFuture.supplyAsync(() -> new ArrayList<>(punishmentCache.values()));
    }

    @Override
    public CompletableFuture<List<BanwaveEntry>> getPendingBanwaveEntries() {
        return CompletableFuture.supplyAsync(() -> 
            banwaveCache.values().stream()
                .filter(e -> !e.isExecuted())
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<Void> executeBanwaveEntry(int id) {
        return CompletableFuture.runAsync(() -> {
            BanwaveEntry entry = banwaveCache.get(id);
            if (entry != null) {
                entry.setExecuted(true);
                
                ConfigurationSection banwaveSection = globalConfig.getConfigurationSection("banwave");
                if (banwaveSection != null) {
                    ConfigurationSection entrySection = banwaveSection.getConfigurationSection(String.valueOf(id));
                    if (entrySection != null) {
                        serializeBanwaveEntry(entrySection, entry);
                        saveGlobalConfig();
                    }
                }
            }
        });
    }

    @Override
    public CompletableFuture<Void> removeBanwaveEntry(int id) {
        return CompletableFuture.runAsync(() -> {
            banwaveCache.remove(id);
            
            ConfigurationSection banwaveSection = globalConfig.getConfigurationSection("banwave");
            if (banwaveSection != null) {
                banwaveSection.set(String.valueOf(id), null);
                saveGlobalConfig();
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> isPlayerBanned(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .anyMatch(p -> p.getTargetUuid().equals(uuid) && 
                             (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN) &&
                             p.getRemovedAt() == null && 
                             (p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now())))
        );
    }

    @Override
    public CompletableFuture<Boolean> isPlayerMuted(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .anyMatch(p -> p.getTargetUuid().equals(uuid) && 
                             (p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMPMUTE) &&
                             p.getRemovedAt() == null && 
                             (p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now())))
        );
    }

    @Override
    public CompletableFuture<Boolean> isIPBanned(String ip) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .anyMatch(p -> ip.equals(p.getTargetIP()) && 
                    (p.getType() == PunishmentType.IPBAN || p.getType() == PunishmentType.IPTEMPBAN) && 
                    p.isActive() && !p.isExpired())
        );
    }

    @Override
    public CompletableFuture<Boolean> isIPMuted(String ip) {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .anyMatch(p -> ip.equals(p.getTargetIP()) && 
                    (p.getType() == PunishmentType.IPMUTE || p.getType() == PunishmentType.IPTEMPMUTE) && 
                    p.isActive() && !p.isExpired())
        );
    }

    @Override
    public CompletableFuture<List<String>> searchPlayers(String query) {
        return CompletableFuture.supplyAsync(() -> 
            playerDataCache.values().stream()
                .map(PlayerData::getPlayerName)
                .filter(name -> name.toLowerCase().contains(query.toLowerCase()))
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<List<PlayerData>> searchPlayersByName(String name) {
        return CompletableFuture.supplyAsync(() -> 
            playerDataCache.values().stream()
                .filter(playerData -> playerData.getPlayerName().toLowerCase().contains(name.toLowerCase()))
                .sorted((p1, p2) -> p2.getLastSeen().compareTo(p1.getLastSeen()))
                .limit(10)
                .collect(Collectors.toList())
        );
    }

    @Override
    public CompletableFuture<Integer> getTotalPunishments() {
        return CompletableFuture.completedFuture(punishmentCache.size());
    }

    @Override
    public CompletableFuture<List<PlayerConnection>> getPlayerConnections(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerConnection> list = new ArrayList<>();
            Map<String, Long> ipMap = playerConnectionsCache.get(uuid);
            if (ipMap == null || ipMap.isEmpty()) return list;
            PlayerData data = playerDataCache.get(uuid);
            String playerName = data != null ? data.getPlayerName() : "Unknown";
            for (Map.Entry<String, Long> e : ipMap.entrySet()) {
                LocalDateTime ts = LocalDateTime.ofEpochSecond(e.getValue() / 1000L, 0, java.time.ZoneOffset.UTC);
                list.add(new PlayerConnectionImpl(uuid, playerName, e.getKey(), ts));
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<List<PlayerConnection>> getPlayersFromIP(String ip) {
        return CompletableFuture.supplyAsync(() -> {
            List<PlayerConnection> list = new ArrayList<>();
            for (Map.Entry<UUID, Map<String, Long>> entry : playerConnectionsCache.entrySet()) {
                Long tsMillis = entry.getValue().get(ip);
                if (tsMillis != null) {
                    UUID uuid = entry.getKey();
                    PlayerData data = playerDataCache.get(uuid);
                    String name = data != null ? data.getPlayerName() : "Unknown";
                    LocalDateTime ts = LocalDateTime.ofEpochSecond(tsMillis / 1000L, 0, java.time.ZoneOffset.UTC);
                    list.add(new PlayerConnectionImpl(uuid, name, ip, ts));
                }
            }
            return list;
        });
    }

    @Override
    public CompletableFuture<Void> recordPlayerConnection(UUID uuid, String playerName, String ip) {
        return CompletableFuture.runAsync(() -> {
            Map<String, Long> ipMap = playerConnectionsCache.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
            long now = System.currentTimeMillis();
            ipMap.put(ip, now);

            PlayerData data = playerDataCache.computeIfAbsent(uuid, u -> new PlayerData(uuid, playerName));
            data.setPlayerName(playerName);
            data.setLastIP(ip);
            data.setLastJoin(now);

            YamlConfiguration playerConfig = getPlayerConfig(uuid);
            serializePlayerData(playerConfig, data);
            serializePlayerConnections(playerConfig, uuid);
            savePlayerConfig(uuid, playerConfig);
        });
    }

    @Override
    public CompletableFuture<List<Punishment>> getAllActivePunishments() {
        return CompletableFuture.supplyAsync(() -> 
            punishmentCache.values().stream()
                .filter(p -> p.getRemovedAt() == null && (p.getExpiresAt() == null || p.getExpiresAt().isAfter(LocalDateTime.now())))
                .collect(Collectors.toList())
        );
    }

    private static class PlayerConnectionImpl implements DatabaseManager.PlayerConnection {
        private final UUID uuid;
        private final String playerName;
        private final String ip;
        private final LocalDateTime timestamp;

        PlayerConnectionImpl(UUID uuid, String playerName, String ip, LocalDateTime timestamp) {
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
