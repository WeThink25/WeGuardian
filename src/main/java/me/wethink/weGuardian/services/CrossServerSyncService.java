package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class CrossServerSyncService {

    private final WeGuardian plugin;
    private final boolean enabled;
    private final int syncInterval;
    private final List<ServerConfig> servers;
    private final boolean syncPunishments;
    private final boolean syncPlayerData;
    private final boolean syncBanwave;
    private final boolean broadcastPunishments;
    private final boolean requireAuthentication;
    private final String authenticationKey;

    public CrossServerSyncService(WeGuardian plugin) {
        this.plugin = plugin;
        
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("cross_server");
        this.enabled = config != null && config.getBoolean("enabled", false);
        this.syncInterval = config != null ? config.getInt("sync_interval", 30) : 30;
        this.syncPunishments = config != null && config.getBoolean("sync_punishments", true);
        this.syncPlayerData = config != null && config.getBoolean("sync_player_data", true);
        this.syncBanwave = config != null && config.getBoolean("sync_banwave", true);
        this.broadcastPunishments = config != null && config.getBoolean("broadcast_punishments", true);
        this.requireAuthentication = config != null && config.getBoolean("require_authentication", false);
        this.authenticationKey = config != null ? config.getString("authentication_key", "") : "";
        
        this.servers = loadServerConfigs(config);
        
        if (enabled) {
            startSyncTask();
            plugin.debug("Cross-server synchronization enabled with %d servers", servers.size());
        } else {
            plugin.debug("Cross-server synchronization disabled");
        }
    }

    private List<ServerConfig> loadServerConfigs(ConfigurationSection config) {
        List<ServerConfig> serverList = new java.util.ArrayList<>();
        if (config != null && config.contains("servers")) {
            List<Map<?, ?>> serverMaps = config.getMapList("servers");
            
            for (Map<?, ?> serverMap : serverMaps) {
                String name = serverMap.containsKey("name") ? (String) serverMap.get("name") : "unknown";
                String host = serverMap.containsKey("host") ? (String) serverMap.get("host") : "localhost";
                int port = serverMap.containsKey("port") ? (Integer) serverMap.get("port") : 25565;
                boolean serverEnabled = serverMap.containsKey("enabled") ? (Boolean) serverMap.get("enabled") : true;
                
                if (serverEnabled) {
                    serverList.add(new ServerConfig(name, host, port));
                    plugin.debug("Loaded server config: %s (%s:%d)", name, host, port);
                } else {
                    plugin.debug("Skipped disabled server: %s", name);
                }
            }
        }
        return serverList;
    }

    private void startSyncTask() {
        if (syncInterval > 0) {
            plugin.debug("Starting cross-server sync task with interval: %d seconds", syncInterval);
            plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
                try {
                    plugin.debug("Starting cross-server synchronization cycle");
                    syncAllData();
                } catch (Exception e) {
                    plugin.getLogger().warning("Cross-server sync task failed: " + e.getMessage());
                    plugin.debug("Cross-server sync task error: %s", e.getMessage());
                }
            }, syncInterval * 20L, syncInterval * 20L);
        }
    }

    public void syncAllData() {
        if (!enabled) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                if (syncPunishments) {
                    syncPunishments();
                }
                if (syncPlayerData) {
                    syncPlayerData();
                }
                if (syncBanwave) {
                    syncBanwave();
                }
                plugin.debug("Cross-server synchronization completed successfully");
            } catch (Exception e) {
                plugin.getLogger().warning("Cross-server synchronization failed: " + e.getMessage());
                plugin.debug("Cross-server sync error: %s", e.getMessage());
            }
        });
    }

    private void syncPunishments() {
        plugin.debug("Syncing punishments across servers");
        
        plugin.getDatabaseManager().getRecentPunishments(100)
                .thenAccept(punishments -> {
                    for (Punishment punishment : punishments) {
                        broadcastPunishment(punishment);
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to sync punishments: " + throwable.getMessage());
                    return null;
                });
    }

    private void syncPlayerData() {
        plugin.debug("Syncing player data across servers");
        
        for (ServerConfig server : servers) {
            syncPlayerDataFromServer(server);
        }
    }

    private void syncBanwave() {
        plugin.debug("Syncing banwave data across servers");
        
        plugin.getDatabaseManager().getPendingBanwaveEntries()
                .thenAccept(entries -> {
                    for (var entry : entries) {
                        broadcastBanwaveEntry(entry);
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().warning("Failed to sync banwave: " + throwable.getMessage());
                    return null;
                });
    }

    public void broadcastPunishment(Punishment punishment) {
        if (!enabled || !broadcastPunishments) return;
        
        plugin.debug("Broadcasting punishment: %s for %s", punishment.getType(), punishment.getTargetName());
        
        for (ServerConfig server : servers) {
            sendPunishmentToServer(server, punishment);
        }
    }

    public void broadcastBanwaveEntry(me.wethink.weGuardian.models.BanwaveEntry entry) {
        if (!enabled) return;
        
        plugin.debug("Broadcasting banwave entry for %s", entry.getTargetName());
        
        for (ServerConfig server : servers) {
            sendBanwaveEntryToServer(server, entry);
        }
    }

    private void sendPunishmentToServer(ServerConfig server, Punishment punishment) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            CompletableFuture.runAsync(() -> {
                try {
                    String json = createPunishmentJson(punishment);
                    sendHttpRequest(server, "/api/punishments", "POST", json);
                    plugin.debug("Sent punishment to server %s", server.name);
                } catch (Exception e) {
                    plugin.debug("Failed to send punishment to server %s: %s", server.name, e.getMessage());
                }
            });
        });
    }

    private void sendBanwaveEntryToServer(ServerConfig server, me.wethink.weGuardian.models.BanwaveEntry entry) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            CompletableFuture.runAsync(() -> {
                try {
                    String json = createBanwaveEntryJson(entry);
                    sendHttpRequest(server, "/api/banwave", "POST", json);
                    plugin.debug("Sent banwave entry to server %s", server.name);
                } catch (Exception e) {
                    plugin.debug("Failed to send banwave entry to server %s: %s", server.name, e.getMessage());
                }
            });
        });
    }

    private void syncPlayerDataFromServer(ServerConfig server) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            CompletableFuture.runAsync(() -> {
                try {
                    String response = sendHttpRequest(server, "/api/players", "GET", null);
                    if (response != null) {
                        processPlayerDataResponse(response);
                        plugin.debug("Synced player data from server %s", server.name);
                    }
                } catch (Exception e) {
                    plugin.debug("Failed to sync player data from server %s: %s", server.name, e.getMessage());
                }
            });
        });
    }

    private String sendHttpRequest(ServerConfig server, String endpoint, String method, String jsonData) throws IOException {
        String urlString = "http://" + server.host + ":" + server.port + endpoint;
        URI uri = URI.create(urlString);
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        
        connection.setRequestMethod(method);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(10000);
        
        if (requireAuthentication && !authenticationKey.isEmpty()) {
            connection.setRequestProperty("Authorization", "Bearer " + authenticationKey);
        }
        
        if (jsonData != null) {
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);
            
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
        }
        
        int responseCode = connection.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return response.toString();
            }
        } else {
            throw new IOException("HTTP error: " + responseCode);
        }
    }

    private String createPunishmentJson(Punishment punishment) {
        return String.format(
                "{\"id\":%d,\"target_uuid\":\"%s\",\"target_name\":\"%s\",\"staff_uuid\":\"%s\",\"staff_name\":\"%s\"," +
                "\"type\":\"%s\",\"reason\":\"%s\",\"created_at\":\"%s\",\"expires_at\":%s,\"active\":%s,\"server_name\":\"%s\"}",
                punishment.getId(),
                punishment.getTargetUuid(),
                punishment.getTargetName(),
                punishment.getStaffUuid(),
                punishment.getStaffName(),
                punishment.getType(),
                punishment.getReason(),
                punishment.getCreatedAt(),
                punishment.getExpiresAt() != null ? "\"" + punishment.getExpiresAt() + "\"" : "null",
                punishment.isActive(),
                punishment.getServerName()
        );
    }

    private String createBanwaveEntryJson(me.wethink.weGuardian.models.BanwaveEntry entry) {
        return String.format(
                "{\"id\":%d,\"target_uuid\":\"%s\",\"target_name\":\"%s\",\"staff_uuid\":\"%s\",\"staff_name\":\"%s\"," +
                "\"reason\":\"%s\",\"created_at\":\"%s\",\"executed\":%s}",
                entry.getId(),
                entry.getTargetUuid(),
                entry.getTargetName(),
                entry.getStaffUuid(),
                entry.getStaffName(),
                entry.getReason(),
                entry.getCreatedAt(),
                entry.isExecuted()
        );
    }

    private void processPlayerDataResponse(String response) {
        plugin.debug("Processing player data response: %s", response);
    }

    public void validateServerConnections() {
        if (!enabled) return;
        
        plugin.debug("Validating cross-server connections...");
        for (ServerConfig server : servers) {
            plugin.getFoliaLib().getScheduler().runNextTick(task -> {
                CompletableFuture.runAsync(() -> {
                    try {
                        String response = sendHttpRequest(server, "/api/health", "GET", null);
                        if (response != null) {
                            plugin.debug("Server %s is reachable", server.name);
                        }
                    } catch (Exception e) {
                        plugin.debug("Server %s is not reachable: %s", server.name, e.getMessage());
                    }
                });
            });
        }
    }

    public void shutdown() {
        plugin.debug("Cross-server synchronization service shutting down");
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<ServerConfig> getServers() {
        return servers;
    }

    public static class ServerConfig {
        public final String name;
        public final String host;
        public final int port;

        public ServerConfig(String name, String host, int port) {
            this.name = name;
            this.host = host;
            this.port = port;
        }
    }
}
