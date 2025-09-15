package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import me.wethink.weGuardian.services.DiscordWebhookService;

public class PunishmentService {

    private final WeGuardian plugin;
    private final DatabaseManager database;
    private final ConcurrentHashMap<UUID, Boolean> banCache;
    private final ConcurrentHashMap<UUID, Boolean> muteCache;
    private final ConcurrentHashMap<UUID, String> playerNameCache;
    private final ConcurrentHashMap<UUID, Long> cacheTimestamps;
    private final ConcurrentHashMap<String, UUID> nameToUuidCache;
    private final ScheduledExecutorService executorService;
    private final ExecutorService asyncExecutor;
    private final AtomicInteger activeOperations;

    private final int maxConcurrentOperations;
    private final int operationTimeout;
    private final boolean cacheEnabled;

    public PunishmentService(WeGuardian plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.banCache = new ConcurrentHashMap<>();
        this.muteCache = new ConcurrentHashMap<>();
        this.playerNameCache = new ConcurrentHashMap<>();
        this.cacheTimestamps = new ConcurrentHashMap<>();
        this.nameToUuidCache = new ConcurrentHashMap<>();
        this.activeOperations = new AtomicInteger(0);

        this.maxConcurrentOperations = plugin.getConfig().getInt("performance.max_concurrent_operations", 50);
        this.operationTimeout = plugin.getConfig().getInt("performance.operation_timeout", 30);
        this.cacheEnabled = plugin.getConfig().getBoolean("performance.cache.enabled", true);

        int threadPoolSize = plugin.getConfig().getInt("performance.thread_pool_size", 4);
        int asyncWorkers = plugin.getConfig().getInt("performance.async.worker_threads", 2);

        this.executorService = Executors.newScheduledThreadPool(threadPoolSize, r -> {
            Thread t = new Thread(r, "WeGuardian-Scheduler");
            t.setDaemon(true);
            return t;
        });

        this.asyncExecutor = Executors.newFixedThreadPool(asyncWorkers, r -> {
            Thread t = new Thread(r, "WeGuardian-Async");
            t.setDaemon(true);
            return t;
        });

        startCacheCleanupTask();
        startPerformanceMonitoring();
    }

    public CompletableFuture<Boolean> executePunishment(PunishmentType type, String targetName, String staffName, String reason, String duration) {
        plugin.debug("Executing punishment: type=%s, target=%s, staff=%s, reason=%s, duration=%s", 
                type, targetName, staffName, reason, duration);
        
        if (activeOperations.get() >= maxConcurrentOperations) {
            plugin.debug("Punishment execution rejected: max concurrent operations reached (%d/%d)", 
                    activeOperations.get(), maxConcurrentOperations);
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.supplyAsync(() -> {
            activeOperations.incrementAndGet();
            plugin.debug("Punishment execution started: active operations now %d", activeOperations.get());
            try {
                boolean result = switch (type) {
                    case BAN -> ban(targetName, staffName, reason).join();
                    case TEMPBAN -> tempban(targetName, staffName, reason, duration).join();
                    case MUTE -> mute(targetName, staffName, reason).join();
                    case TEMPMUTE -> tempmute(targetName, staffName, reason, duration).join();
                    case KICK -> kick(targetName, staffName, reason).join();
                    case WARN -> warn(targetName, staffName, reason).join();
                    case IPBAN -> ipban(targetName, staffName, reason).join();
                    case IPMUTE -> ipmute(targetName, staffName, reason).join();
                    default -> false;
                };
                plugin.debug("Punishment execution completed: result=%s", result);
                return result;
            } finally {
                activeOperations.decrementAndGet();
                plugin.debug("Punishment execution finished: active operations now %d", activeOperations.get());
            }
        }, asyncExecutor).orTimeout(operationTimeout, TimeUnit.SECONDS);
    }

    public CompletableFuture<Boolean> ban(String targetName, String staffName, String reason) {
        plugin.debug("Starting ban process for player: %s", targetName);
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                plugin.debug("Ban failed: player not found - %s", targetName);
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);
                plugin.debug("Ban process: targetUuid=%s, finalTargetName=%s, staffUuid=%s", 
                        targetUuid, finalTargetName, staffUuid);

                if (cacheEnabled && isCacheValid(targetUuid) && banCache.getOrDefault(targetUuid, false)) {
                    plugin.debug("Ban skipped: player already banned (cached) - %s", finalTargetName);
                    return CompletableFuture.completedFuture(false);
                }

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.BAN, reason, null, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        plugin.debug("Ban punishment added to database with ID: %d", punishmentId);
                        if (cacheEnabled) {
                            banCache.put(targetUuid, true);
                            updateCacheTimestamp(targetUuid);
                            nameToUuidCache.put(finalTargetName.toLowerCase(), targetUuid);
                            plugin.debug("Ban cache updated for player: %s", finalTargetName);
                        }

                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            plugin.debug("Kicking online player: %s", finalTargetName);
                            String kickMessage = getKickMessage(punishment);
                            Component kickComponent = MessageUtils.toComponent(kickMessage);
                            plugin.getFoliaLib().getScheduler().runAtEntity(targetPlayer, task -> {
                                if (targetPlayer.isOnline()) {
                                    targetPlayer.kick(kickComponent);
                                }
                            });
                        } else {
                            plugin.debug("Player not online, skipping kick: %s", finalTargetName);
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        plugin.debug("Ban completed successfully for player: %s", finalTargetName);
                        return CompletableFuture.completedFuture(true);
                    }
                    plugin.debug("Ban failed: could not add punishment to database");
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> tempban(String targetName, String staffName, String reason, String duration) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);

                if (cacheEnabled && banCache.getOrDefault(targetUuid, false)) {
                    return CompletableFuture.completedFuture(false);
                }

                LocalDateTime expiresAt = TimeUtils.parseTime(duration);
                if (expiresAt == null) {
                    return CompletableFuture.completedFuture(false);
                }

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.TEMPBAN, reason, expiresAt, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        if (cacheEnabled) {
                            banCache.put(targetUuid, true);
                            updateCacheTimestamp(targetUuid);
                            nameToUuidCache.put(finalTargetName.toLowerCase(), targetUuid);
                        }

                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            String kickMessage = getKickMessage(punishment);
                            Component kickComponent = MessageUtils.toComponent(kickMessage);
                            plugin.getFoliaLib().getScheduler().runAtEntity(targetPlayer, task -> {
                                if (targetPlayer.isOnline()) {
                                    targetPlayer.kick(kickComponent);
                                }
                            });
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        var webhook = new DiscordWebhookService(plugin);
                        webhook.sendPunishmentWebhook(punishment);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> mute(String targetName, String staffName, String reason) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);

                if (cacheEnabled && muteCache.getOrDefault(targetUuid, false)) {
                    return CompletableFuture.completedFuture(false);
                }

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.MUTE, reason, null, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        if (cacheEnabled) {
                            muteCache.put(targetUuid, true);
                        }

                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            String actionBarMessage = getActionBarMessage(punishment);
                            if (!actionBarMessage.isEmpty()) {
                                Component component = MessageUtils.toComponent(actionBarMessage);
                                targetPlayer.sendActionBar(component);
                            }
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        var webhook = new DiscordWebhookService(plugin);
                        webhook.sendPunishmentWebhook(punishment);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> tempmute(String targetName, String staffName, String reason, String duration) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);

                if (cacheEnabled && muteCache.getOrDefault(targetUuid, false)) {
                    return CompletableFuture.completedFuture(false);
                }

                LocalDateTime expiresAt = TimeUtils.parseTime(duration);
                if (expiresAt == null) {
                    return CompletableFuture.completedFuture(false);
                }

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.TEMPMUTE, reason, expiresAt, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        if (cacheEnabled) {
                            muteCache.put(targetUuid, true);
                        }

                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            String actionBarMessage = getActionBarMessage(punishment);
                            if (!actionBarMessage.isEmpty()) {
                                Component component = MessageUtils.toComponent(actionBarMessage);
                                targetPlayer.sendActionBar(component);
                            }
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        var webhook = new DiscordWebhookService(plugin);
                        webhook.sendPunishmentWebhook(punishment);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> kick(String targetName, String staffName, String reason) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.KICK, reason, null, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            String kickMessage = getKickMessage(punishment);
                            Component kickComponent = MessageUtils.toComponent(kickMessage);
                            plugin.getFoliaLib().getScheduler().runAtEntity(targetPlayer, task -> {
                                if (targetPlayer.isOnline()) {
                                    targetPlayer.kick(kickComponent);
                                }
                            });
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        var webhook = new DiscordWebhookService(plugin);
                        webhook.sendPunishmentWebhook(punishment);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> warn(String targetName, String staffName, String reason) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return getPlayerName(targetUuid).thenCompose(finalTargetName -> {
                UUID staffUuid = getStaffUuid(staffName);

                Punishment punishment = new Punishment(
                        targetUuid, finalTargetName, staffUuid, staffName,
                        PunishmentType.WARN, reason, null, getServerName()
                );

                return database.addPunishment(punishment).thenCompose(punishmentId -> {
                    if (punishmentId > 0) {
                        Player targetPlayer = Bukkit.getPlayer(targetUuid);
                        if (targetPlayer != null && targetPlayer.isOnline()) {
                            String warnMessage = getWarnMessage(punishment);
                            Component warnComponent = MessageUtils.toComponent(warnMessage);
                            targetPlayer.sendMessage(warnComponent);
                        }

                        plugin.getNotificationService().broadcastPunishment(punishment);
                        var webhook = new DiscordWebhookService(plugin);
                        webhook.sendPunishmentWebhook(punishment);
                        return CompletableFuture.completedFuture(true);
                    }
                    return CompletableFuture.completedFuture(false);
                });
            });
        });
    }

    public CompletableFuture<Boolean> unban(String targetName, String staffName) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return database.getActivePunishments(targetUuid).thenCompose(punishments -> {
                Punishment banPunishment = punishments.stream()
                        .filter(p -> (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN) && !p.isExpired())
                        .findFirst()
                        .orElse(null);

                if (banPunishment == null) {
                    return CompletableFuture.completedFuture(false);
                }

                UUID staffUuid = getStaffUuid(staffName);
                return database.removePunishment(banPunishment.getId(), staffUuid, staffName, "Unbanned by " + staffName).thenApply(success -> {
                    if (cacheEnabled) {
                        banCache.remove(targetUuid);
                    }
                    plugin.getNotificationService().broadcastUnpunishment(banPunishment, staffName);
                    var webhook = new DiscordWebhookService(plugin);
                    webhook.sendUnpunishmentWebhook(banPunishment, staffName);
                    return true;
                });
            });
        });
    }

    public CompletableFuture<Boolean> unmute(String targetName, String staffName) {
        return executePlayerLookup(targetName).thenCompose(targetUuid -> {
            if (targetUuid == null) {
                return CompletableFuture.completedFuture(false);
            }

            return database.getActivePunishments(targetUuid).thenCompose(punishments -> {
                Punishment mutePunishment = punishments.stream()
                        .filter(p -> (p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMPMUTE) && !p.isExpired())
                        .findFirst()
                        .orElse(null);

                if (mutePunishment == null) {
                    return CompletableFuture.completedFuture(false);
                }

                UUID staffUuid = getStaffUuid(staffName);
                return database.removePunishment(mutePunishment.getId(), staffUuid, staffName, "Unmuted by " + staffName).thenApply(success -> {
                    if (cacheEnabled) {
                        muteCache.remove(targetUuid);
                    }
                    plugin.getNotificationService().broadcastUnpunishment(mutePunishment, staffName);
                    var webhook = new DiscordWebhookService(plugin);
                    webhook.sendUnpunishmentWebhook(mutePunishment, staffName);
                    return true;
                });
            });
        });
    }

    public CompletableFuture<Boolean> ipban(String ipAddress, String staffName, String reason) {
        return executeIPLookup(ipAddress).thenCompose(validIP -> {
            if (validIP == null) {
                return CompletableFuture.completedFuture(false);
            }

            UUID staffUuid = getStaffUuid(staffName);

            Punishment punishment = new Punishment(
                    validIP,
                    staffUuid,
                    staffName,
                    PunishmentType.IPBAN,
                    reason,
                    null,
                    plugin.getServer().getName()
            );

            return database.addPunishment(punishment).thenCompose(punishmentId -> {
                if (punishmentId > 0) {
                    kickPlayersWithIP(ipAddress, punishment);
                    plugin.getNotificationService().broadcastPunishment(punishment);
                    return CompletableFuture.completedFuture(true);
                }
                return CompletableFuture.completedFuture(false);
            });
        });
    }

    public CompletableFuture<Boolean> ipmute(String ipAddress, String staffName, String reason) {
        return executeIPLookup(ipAddress).thenCompose(validIP -> {
            if (validIP == null) {
                return CompletableFuture.completedFuture(false);
            }

            UUID staffUuid = getStaffUuid(staffName);

            Punishment punishment = new Punishment(
                    validIP,
                    staffUuid,
                    staffName,
                    PunishmentType.IPMUTE,
                    reason,
                    null,
                    plugin.getServer().getName()
            );

            return database.addPunishment(punishment).thenCompose(punishmentId -> {
                if (punishmentId > 0) {
                    notifyPlayersWithIP(ipAddress, punishment);
                    plugin.getNotificationService().broadcastPunishment(punishment);
                    return CompletableFuture.completedFuture(true);
                }
                return CompletableFuture.completedFuture(false);
            });
        });
    }

    public CompletableFuture<Boolean> ipMute(String ip, String staffName, String reason) {
        return ipmute(ip, staffName, reason);
    }

    public CompletableFuture<Boolean> ipMute(String ip, String staffName, String reason, String duration) {
        if (duration != null && !duration.isEmpty()) {
            return ipTempmute(ip, staffName, reason, duration);
        } else {
            return ipmute(ip, staffName, reason);
        }
    }

    public CompletableFuture<Boolean> ipTempmute(String ip, String staffName, String reason, String duration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                LocalDateTime expires = null;
                if (duration != null && !duration.isEmpty()) {
                    expires = TimeUtils.parseTimeToExpiration(duration);
                }

                UUID staffUuid = getStaffUuid(staffName);

                Punishment punishment = new Punishment(
                        ip,
                        staffUuid,
                        staffName,
                        PunishmentType.IPMUTE,
                        reason,
                        expires,
                        plugin.getServer().getName()
                );

                database.addPunishment(punishment);

                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getAddress().getAddress().getHostAddress().equals(ip)) {
                        player.sendMessage(MessageUtils.colorize("&cYour IP has been muted for: " + reason));
                    }
                }

                return true;
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to IP mute " + ip + ": " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<UUID> executePlayerLookup(String playerName) {
        Player onlinePlayer = Bukkit.getPlayer(playerName);
        if (onlinePlayer != null) {
            UUID uuid = onlinePlayer.getUniqueId();
            if (cacheEnabled) {
                playerNameCache.put(uuid, playerName);
            }
            return CompletableFuture.completedFuture(uuid);
        }

        if (cacheEnabled) {
            for (Map.Entry<UUID, String> entry : playerNameCache.entrySet()) {
                if (entry.getValue().equalsIgnoreCase(playerName)) {
                    return CompletableFuture.completedFuture(entry.getKey());
                }
            }
        }

        try {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerName);
            if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
                UUID uuid = offlinePlayer.getUniqueId();
                if (cacheEnabled) {
                    playerNameCache.put(uuid, playerName);
                }
                return CompletableFuture.completedFuture(uuid);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get UUID for player " + playerName + ": " + e.getMessage());
        }

        return database.getPlayerData(playerName).thenApply(playerData -> {
            if (playerData != null) {
                if (cacheEnabled) {
                    playerNameCache.put(playerData.getUuid(), playerName);
                }
                return playerData.getUuid();
            }
            return null;
        });
    }

    public CompletableFuture<String> getPlayerName(UUID playerUuid) {
        if (cacheEnabled && playerNameCache.containsKey(playerUuid)) {
            return CompletableFuture.completedFuture(playerNameCache.get(playerUuid));
        }

        return database.getPlayerData(playerUuid).thenApply(playerData -> {
            if (playerData != null) {
                if (cacheEnabled) {
                    playerNameCache.put(playerUuid, playerData.getName());
                }
                return playerData.getName();
            }

            try {
                String name = Bukkit.getOfflinePlayer(playerUuid).getName();
                if (name != null && cacheEnabled) {
                    playerNameCache.put(playerUuid, name);
                }
                return name != null ? name : "Unknown";
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to get name for UUID " + playerUuid + ": " + e.getMessage());
                return "Unknown";
            }
        });
    }

    public CompletableFuture<String> executeIPLookup(String ipAddress) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                InetAddress.getByName(ipAddress);
                return ipAddress;
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid IP address: " + ipAddress);
                return null;
            }
        }, asyncExecutor);
    }

    public void kickPlayersWithIP(String ipAddress, Punishment punishment) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = player.getAddress();
                if (address != null && address.getAddress().getHostAddress().equals(ipAddress)) {
                    String kickMessage = getKickMessage(punishment);
                    Component kickComponent = MessageUtils.toComponent(kickMessage);
                    player.kick(kickComponent);
                }
            }
        });
    }

    public void notifyPlayersWithIP(String ipAddress, Punishment punishment) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                InetSocketAddress address = player.getAddress();
                if (address != null && address.getAddress().getHostAddress().equals(ipAddress)) {
                    String actionBarMessage = getActionBarMessage(punishment);
                    if (!actionBarMessage.isEmpty()) {
                        Component component = MessageUtils.toComponent(actionBarMessage);
                        player.sendActionBar(component);
                    }
                }
            }
        });
    }

    private void startCacheCleanupTask() {
        if (!cacheEnabled) return;

        int cleanupInterval = plugin.getConfig().getInt("performance.cache.cleanup_interval", 300);
        int maxCacheSize = plugin.getConfig().getInt("performance.cache.max_cache_size", 10000);

        plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
            try {
                plugin.debug("Starting cache cleanup task");
                int banCacheSize = banCache.size();
                int muteCacheSize = muteCache.size();
                int nameCacheSize = playerNameCache.size();
                
                if (banCacheSize > maxCacheSize) {
                    plugin.debug("Clearing ban cache: size %d > max %d", banCacheSize, maxCacheSize);
                    banCache.clear();
                }
                if (muteCacheSize > maxCacheSize) {
                    plugin.debug("Clearing mute cache: size %d > max %d", muteCacheSize, maxCacheSize);
                    muteCache.clear();
                }
                if (nameCacheSize > maxCacheSize) {
                    plugin.debug("Clearing name cache: size %d > max %d", nameCacheSize, maxCacheSize);
                    playerNameCache.clear();
                }

                if (plugin.getConfig().getBoolean("debug.log_cache_statistics", false)) {
                    plugin.getLogger().info("Cache cleanup completed. Sizes - Ban: " + banCache.size() +
                            ", Mute: " + muteCache.size() +
                            ", Names: " + playerNameCache.size());
                }
                plugin.debug("Cache cleanup completed successfully");
            } catch (Exception e) {
                plugin.getLogger().severe("Error during cache cleanup: " + e.getMessage());
                plugin.debug("Cache cleanup error: %s", e.getMessage());
            }
        }, cleanupInterval * 20, cleanupInterval * 20);
    }

    private void startPerformanceMonitoring() {
        if (!plugin.getConfig().getBoolean("debug.log_performance_metrics", false)) return;

        plugin.getFoliaLib().getScheduler().runTimerAsync(() -> {
            try {
                int active = activeOperations.get();
                plugin.debug("Performance monitoring: active operations %d/%d", active, maxConcurrentOperations);
                plugin.getLogger().info("Performance Metrics - Active Operations: " + active +
                        "/" + maxConcurrentOperations);
            } catch (Exception e) {
                plugin.getLogger().severe("Error during performance monitoring: " + e.getMessage());
                plugin.debug("Performance monitoring error: %s", e.getMessage());
            }
        }, 60 * 20, 60 * 20);
    }

    private boolean isCacheValid(UUID uuid) {
        if (!cacheEnabled) return false;
        Long timestamp = cacheTimestamps.get(uuid);
        if (timestamp == null) return false;
        
        int expireAfterAccess = plugin.getConfig().getInt("performance.cache.expire_after_access", 1800);
        return (System.currentTimeMillis() - timestamp) < (expireAfterAccess * 1000L);
    }

    private void updateCacheTimestamp(UUID uuid) {
        if (cacheEnabled) {
            cacheTimestamps.put(uuid, System.currentTimeMillis());
        }
    }

    private void invalidateCache(UUID uuid) {
        banCache.remove(uuid);
        muteCache.remove(uuid);
        cacheTimestamps.remove(uuid);
        plugin.debug("Cache invalidated for UUID: %s", uuid);
    }

    private void invalidateCache(String playerName) {
        UUID uuid = nameToUuidCache.get(playerName.toLowerCase());
        if (uuid != null) {
            invalidateCache(uuid);
        }
        nameToUuidCache.remove(playerName.toLowerCase());
    }

    private UUID getStaffUuid(String staffName) {
        Player staffPlayer = Bukkit.getPlayer(staffName);
        return staffPlayer != null ? staffPlayer.getUniqueId() : UUID.nameUUIDFromBytes(("Console:" + staffName).getBytes());
    }

    private String getServerName() {
        return plugin.getConfig().getString("server_name", "Unknown");
    }

    private String getKickMessage(Punishment punishment) {
        String template = plugin.getMessage("screen." + punishment.getType().name().toLowerCase(),
                getDefaultKickMessage(punishment.getType()));
        
        String appealUrl = plugin.getAppealUrl();
        
        return MessageUtils.formatPunishmentMessage(template, punishment, appealUrl);
    }

    private String getActionBarMessage(Punishment punishment) {
        String messageKey = "actionbar." + punishment.getType().name().toLowerCase();
        String template = plugin.getMessage(messageKey, getDefaultActionBarMessage(punishment.getType()));
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    private String getWarnMessage(Punishment punishment) {
        String template = plugin.getMessage("screen.warn",
                "&e⚠ &6You have been warned!\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Date: &f{date}");
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    private String getDefaultKickMessage(PunishmentType type) {
        return switch (type) {
            case BAN -> "&c&l⚠ YOU ARE BANNED ⚠\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Date: &f{date}\n&7Appeal: &f{appeal-url}";
            case TEMPBAN -> "&e&l⏰ TEMPORARY BAN ⏰\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Expires: &f{expires}\n&7Appeal: &f{appeal-url}";
            case KICK -> "&c&l⚡ KICKED ⚡\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Appeal: &f{appeal-url}";
            default -> "&cYou have been punished: {reason}\n&7Appeal: &f{appeal-url}";
        };
    }

    private String getDefaultActionBarMessage(PunishmentType type) {
        return switch (type) {
            case MUTE -> "&6🔇 You are muted: &f{reason}";
            case TEMPMUTE -> "&6🔇 Muted: &f{reason} &7| &fExpires: {time-left}";
            default -> "&7You are punished: {reason}";
        };
    }

    public void shutdown() {
        executorService.shutdown();
        asyncExecutor.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public CompletableFuture<Boolean> isBanned(UUID playerUuid) {
        if (cacheEnabled && banCache.containsKey(playerUuid)) {
            return CompletableFuture.completedFuture(banCache.get(playerUuid));
        }
        return database.isPlayerBanned(playerUuid);
    }

    public CompletableFuture<Boolean> isPlayerBanned(UUID playerUuid) {
        return isBanned(playerUuid);
    }

    public CompletableFuture<Boolean> isMuted(UUID playerUuid) {
        if (cacheEnabled && muteCache.containsKey(playerUuid)) {
            return CompletableFuture.completedFuture(muteCache.get(playerUuid));
        }
        return database.isPlayerMuted(playerUuid);
    }

    public CompletableFuture<Boolean> isPlayerMuted(UUID playerUuid) {
        return isMuted(playerUuid);
    }

    public CompletableFuture<List<Punishment>> getActivePunishments(UUID playerUuid) {
        return database.getActivePunishments(playerUuid);
    }

    public CompletableFuture<List<Punishment>> getPunishmentHistory(UUID playerUuid) {
        return database.getPunishmentHistory(playerUuid);
    }

    public CompletableFuture<Boolean> canPunish(UUID staffUuid, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Player staff = Bukkit.getPlayer(staffUuid);
            Player target = Bukkit.getPlayer(targetUuid);

            if (target == null) {
                return true; 
            }

            if (target.hasPermission("weguardian.bypass") && !staff.hasPermission("weguardian.bypass.override")) {
                return false;
            }

            if (target.hasPermission("weguardian.admin") && !staff.hasPermission("weguardian.owner")) {
                return false;
            }

            if (target.hasPermission("weguardian.moderator") && !staff.hasPermission("weguardian.admin")) {
                return false;
            }

            return true;
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> canPunish(CommandSender staff, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer(targetUuid);

            if (target == null) {
                return true; 
            }

            if (target.hasPermission("weguardian.bypass")) {
                return false;
            }

            if (target.hasPermission("weguardian.admin") && !staff.hasPermission("weguardian.owner")) {
                return false;
            }

            return true;
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> canPunish(CommandSender staff, String targetName) {
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            return CompletableFuture.completedFuture(true); 
        }

        if (target.hasPermission("weguardian.bypass")) {
            return CompletableFuture.completedFuture(false);
        }

        if (target.hasPermission("weguardian.admin") && !staff.hasPermission("weguardian.owner")) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(true);
    }

    public CompletableFuture<UUID> getPlayerUUID(String playerName) {
        return executePlayerLookup(playerName);
    }

    public CompletableFuture<Boolean> banPlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                if (plugin.getDatabaseManager().isPlayerBanned(targetUuid).join()) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already banned."));
                    return false;
                }

                return ban(targetName, sender.getName(), reason).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in banPlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> tempbanPlayer(CommandSender sender, UUID targetUuid, String reason, String duration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                if (plugin.getDatabaseManager().isPlayerBanned(targetUuid).join()) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already banned."));
                    return false;
                }

                return tempban(targetName, sender.getName(), reason, duration).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in tempbanPlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> mutePlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                if (plugin.getDatabaseManager().isPlayerMuted(targetUuid).join()) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already muted."));
                    return false;
                }

                return mute(targetName, sender.getName(), reason).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in mutePlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> tempmutePlayer(CommandSender sender, UUID targetUuid, String reason, String duration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                if (plugin.getDatabaseManager().isPlayerMuted(targetUuid).join()) {
                    sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already muted."));
                    return false;
                }

                return tempmute(targetName, sender.getName(), reason, duration).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in tempmutePlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> kickPlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                return kick(targetName, sender.getName(), reason).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in kickPlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> warnPlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                return warn(targetName, sender.getName(), reason).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in warnPlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> unbanPlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                return unban(targetName, sender.getName()).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in unbanPlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> unmutePlayer(CommandSender sender, UUID targetUuid, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (!canPunish(sender, targetUuid).join()) {
                    return false;
                }

                String targetName = getPlayerName(targetUuid).join();
                if (targetName == null) {
                    return false;
                }

                return unmute(targetName, sender.getName()).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error in unmutePlayer: " + e.getMessage());
                return false;
            }
        }, asyncExecutor);
    }

    public CompletableFuture<Boolean> banPlayer(UUID targetUuid, String targetName, String staffName, String reason) {
        return ban(targetName, staffName, reason);
    }

    public CompletableFuture<Boolean> tempbanPlayer(UUID targetUuid, String targetName, String staffName, String reason, String duration) {
        return tempban(targetName, staffName, reason, duration);
    }

    public CompletableFuture<Boolean> mutePlayer(UUID targetUuid, String targetName, String staffName, String reason) {
        return mute(targetName, staffName, reason);
    }

    public CompletableFuture<Boolean> tempmutePlayer(UUID targetUuid, String targetName, String staffName, String reason, String duration) {
        return tempmute(targetName, staffName, reason, duration);
    }

    public CompletableFuture<Boolean> kickPlayer(UUID targetUuid, String targetName, String staffName, String reason) {
        return kick(targetName, staffName, reason);
    }

    public CompletableFuture<Boolean> warnPlayer(UUID targetUuid, String targetName, String staffName, String reason) {
        return warn(targetName, staffName, reason);
    }

    public CompletableFuture<Boolean> unbanPlayer(UUID targetUuid, String targetName, String staffName) {
        return unban(targetName, staffName);
    }

    public CompletableFuture<Boolean> unmutePlayer(UUID targetUuid, String targetName, String staffName) {
        return unmute(targetName, staffName);
    }

    public CompletableFuture<Boolean> executeBan(CommandSender sender, String targetName, String reason, String duration, boolean silent, String templateName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.ban")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            if (templateName != null && !templateName.isEmpty()) {
                return plugin.getTemplateService().executeTemplate(sender, targetName, templateName, duration)
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Error executing template: " + throwable.getMessage());
                            sender.sendMessage(MessageUtils.colorize("&cError executing template: " + throwable.getMessage()));
                            return false;
                        }).join();
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (plugin.getDatabaseManager().isPlayerBanned(targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already banned."));
                return false;
            }

            if (sender instanceof Player && !canPunish((Player) sender, targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return (duration != null && !duration.isEmpty()) ?
                    tempban(targetName, staffName, reason, duration).join() :
                    ban(targetName, staffName, reason).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing ban: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the ban."));
            return false;
        });
    }

    public CompletableFuture<Boolean> executeKick(CommandSender sender, String targetName, String reason, boolean silent, String templateName, boolean ipKick) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.kick")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            if (templateName != null && !templateName.isEmpty()) {
                return plugin.getTemplateService().executeTemplate(sender, targetName, templateName, null)
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Error executing template: " + throwable.getMessage());
                            sender.sendMessage(MessageUtils.colorize("&cError executing template: " + throwable.getMessage()));
                            return false;
                        }).join();
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (sender instanceof Player && !canPunish((Player) sender, targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return kick(targetName, staffName, reason).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing kick: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the kick."));
            return false;
        });
    }

    public CompletableFuture<Boolean> executeMute(CommandSender sender, String targetName, String reason, String duration, boolean silent, String templateName, boolean ipMute) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.mute")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            if (templateName != null && !templateName.isEmpty()) {
                return plugin.getTemplateService().executeTemplate(sender, targetName, templateName, duration)
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Error executing template: " + throwable.getMessage());
                            sender.sendMessage(MessageUtils.colorize("&cError executing template: " + throwable.getMessage()));
                            return false;
                        }).join();
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (plugin.getDatabaseManager().isPlayerMuted(targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer " + targetName + " is already muted."));
                return false;
            }

            if (sender instanceof Player && !canPunish((Player) sender, targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return (duration != null && !duration.isEmpty()) ?
                    tempmute(targetName, staffName, reason, duration).join() :
                    mute(targetName, staffName, reason).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing mute: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the mute."));
            return false;
        });
    }

    public CompletableFuture<Boolean> executeWarn(CommandSender sender, String targetName, String reason, boolean silent, String templateName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.warn")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            if (templateName != null && !templateName.isEmpty()) {
                return plugin.getTemplateService().executeTemplate(sender, targetName, templateName, null)
                        .exceptionally(throwable -> {
                            plugin.getLogger().severe("Error executing template: " + throwable.getMessage());
                            sender.sendMessage(MessageUtils.colorize("&cError executing template: " + throwable.getMessage()));
                            return false;
                        }).join();
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (sender instanceof Player && !canPunish((Player) sender, targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return warn(targetName, staffName, reason).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing warn: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the warn."));
            return false;
        });
    }

    public CompletableFuture<Boolean> executeUnban(CommandSender sender, String targetName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.unban")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (!plugin.getDatabaseManager().isPlayerBanned(targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&c" + targetName + " is not currently banned."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return unban(targetName, staffName).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing unban: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the unban."));
            return false;
        });
    }

    public CompletableFuture<Boolean> executeUnmute(CommandSender sender, String targetName) {
        return CompletableFuture.supplyAsync(() -> {
            if (!sender.hasPermission("weguardian.unmute")) {
                sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
                return false;
            }

            UUID targetUuid = getPlayerUUID(targetName).join();
            if (targetUuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return false;
            }

            if (!plugin.getDatabaseManager().isPlayerMuted(targetUuid).join()) {
                sender.sendMessage(MessageUtils.colorize("&c" + targetName + " is not currently muted."));
                return false;
            }

            String staffName = sender instanceof Player ? sender.getName() : "Console";
            return unmute(targetName, staffName).join();
        }).exceptionally(throwable -> {
            plugin.getLogger().severe("Error executing unmute: " + throwable.getMessage());
            sender.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the unmute."));
            return false;
        });
    }

    public CompletableFuture<Boolean> canPunish(Player staff, UUID targetUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer(targetUuid);

            if (target == null) {
                return true; 
            }

            if (target.hasPermission("weguardian.bypass") && !staff.hasPermission("weguardian.bypass.override")) {
                return false;
            }

            if (target.hasPermission("weguardian.admin") && !staff.hasPermission("weguardian.owner")) {
                return false;
            }

            if (target.hasPermission("weguardian.moderator") && !staff.hasPermission("weguardian.admin")) {
                return false;
            }

            return true;
        }, asyncExecutor);
    }
}
