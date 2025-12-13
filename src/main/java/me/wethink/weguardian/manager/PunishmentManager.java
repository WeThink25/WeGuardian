package me.wethink.weguardian.manager;

import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.cache.CacheManager;
import me.wethink.weguardian.database.PunishmentDAO;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


public class PunishmentManager {

    private final WeGuardian plugin;
    private final PunishmentDAO punishmentDAO;
    private final CacheManager cacheManager;

    public PunishmentManager(WeGuardian plugin, PunishmentDAO punishmentDAO, CacheManager cacheManager) {
        this.plugin = plugin;
        this.punishmentDAO = punishmentDAO;
        this.cacheManager = cacheManager;

        startExpiryTask();
    }


    private void startExpiryTask() {
        int interval = plugin.getConfig().getInt("cache.expiry-check-interval", 60);

        plugin.getSchedulerManager().runAsyncRepeating(() -> {
            punishmentDAO.cleanupExpired().thenAccept(count -> {
                if (count > 0) {
                    plugin.getLogger().info("Cleaned up " + count + " expired punishments.");
                    cacheManager.clear();
                }
            });
        }, interval, interval, TimeUnit.SECONDS);
    }



    public CompletableFuture<Punishment> ban(UUID targetUUID, String targetName, UUID staffUUID,
            String staffName, String reason) {
        return createPunishment(targetUUID, targetName, staffUUID, staffName,
                PunishmentType.BAN, reason, null);
    }


    public CompletableFuture<Punishment> tempban(UUID targetUUID, String targetName, UUID staffUUID,
            String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return createPunishment(targetUUID, targetName, staffUUID, staffName,
                PunishmentType.TEMPBAN, reason, expiresAt);
    }


    public CompletableFuture<Boolean> unban(UUID targetUUID, UUID staffUUID, String staffName, String reason) {
        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

        return punishmentDAO.deactivate(targetUUID, staffUUID, staffName, reason != null ? reason : "Unbanned",
                PunishmentType.BAN, PunishmentType.TEMPBAN)
                .thenApply(count -> {
                    if (count > 0) {
                        cacheManager.invalidateBan(targetUUID);

                        plugin.getWebhookManager().logUnban(targetName != null ? targetName : targetUUID.toString(),
                                staffName);
                        return true;
                    }
                    return false;
                });
    }



    public CompletableFuture<Punishment> banIp(UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID, String staffName, String reason) {
        return createIpPunishment(targetUUID, targetName, ipAddress, staffUUID, staffName,
                PunishmentType.BANIP, reason, null);
    }


    public CompletableFuture<Punishment> tempbanIp(UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return createIpPunishment(targetUUID, targetName, ipAddress, staffUUID, staffName,
                PunishmentType.TEMPBANIP, reason, expiresAt);
    }


    public CompletableFuture<Boolean> unbanIp(String ipAddress, UUID staffUUID, String staffName, String reason) {
        return punishmentDAO.deactivateIp(ipAddress, staffUUID, staffName, reason != null ? reason : "Unbanned",
                PunishmentType.BANIP, PunishmentType.TEMPBANIP)
                .thenApply(count -> {
                    if (count > 0) {
                        cacheManager.invalidateIpBan(ipAddress);
                        // Log to Discord
                        plugin.getWebhookManager().logUnbanIp(ipAddress, "Unknown", staffName);
                        return true;
                    }
                    return false;
                });
    }


    public CompletableFuture<Punishment> muteIp(UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID, String staffName, String reason) {
        return createIpPunishment(targetUUID, targetName, ipAddress, staffUUID, staffName,
                PunishmentType.MUTEIP, reason, null);
    }


    public CompletableFuture<Punishment> tempmuteIp(UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID, String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return createIpPunishment(targetUUID, targetName, ipAddress, staffUUID, staffName,
                PunishmentType.TEMPMUTEIP, reason, expiresAt);
    }


    public CompletableFuture<Boolean> unmuteIp(String ipAddress, UUID staffUUID, String staffName, String reason) {
        return punishmentDAO.deactivateIp(ipAddress, staffUUID, staffName, reason != null ? reason : "Unmuted",
                PunishmentType.MUTEIP, PunishmentType.TEMPMUTEIP)
                .thenApply(count -> {
                    if (count > 0) {
                        cacheManager.invalidateIpMute(ipAddress);
                        plugin.getWebhookManager().logUnmuteIp(ipAddress, "Unknown", staffName);
                        return true;
                    }
                    return false;
                });
    }



    public CompletableFuture<Punishment> mute(UUID targetUUID, String targetName, UUID staffUUID,
            String staffName, String reason) {
        return createPunishment(targetUUID, targetName, staffUUID, staffName,
                PunishmentType.MUTE, reason, null);
    }


    public CompletableFuture<Punishment> tempmute(UUID targetUUID, String targetName, UUID staffUUID,
            String staffName, long durationMillis, String reason) {
        Instant expiresAt = TimeUtil.getExpiryInstant(durationMillis);
        return createPunishment(targetUUID, targetName, staffUUID, staffName,
                PunishmentType.TEMPMUTE, reason, expiresAt);
    }


    public CompletableFuture<Boolean> unmute(UUID targetUUID, UUID staffUUID, String staffName, String reason) {
        String targetName = Bukkit.getOfflinePlayer(targetUUID).getName();

        return punishmentDAO.deactivate(targetUUID, staffUUID, staffName, reason != null ? reason : "Unmuted",
                PunishmentType.MUTE, PunishmentType.TEMPMUTE)
                .thenApply(count -> {
                    if (count > 0) {
                        cacheManager.invalidateMute(targetUUID);
                        plugin.getWebhookManager().logUnmute(targetName != null ? targetName : targetUUID.toString(),
                                staffName);
                        return true;
                    }
                    return false;
                });
    }


    public CompletableFuture<Boolean> kick(UUID targetUUID, String targetName, UUID staffUUID,
            String staffName, String reason) {
        return CompletableFuture.supplyAsync(() -> {
            Player target = Bukkit.getPlayer(targetUUID);
            if (target == null || !target.isOnline()) {
                return false;
            }

            Punishment punishment = new Punishment(targetUUID, targetName, staffUUID, staffName,
                    PunishmentType.KICK, reason, Instant.now(), Instant.now());
            punishment.setActive(false); // Kicks are instant, not ongoing

            punishmentDAO.insert(punishment);

            plugin.getWebhookManager().logPunishment(punishment);

            String kickMessage = buildKickMessage(PunishmentType.KICK, reason, null);
            plugin.getSchedulerManager().runForEntity(target, () -> {
                target.kick(MessageUtil.toComponent(kickMessage));
            });

            return true;
        });
    }


    public CompletableFuture<Optional<Punishment>> getActiveBan(UUID targetUUID) {
        Optional<Punishment> cached = cacheManager.getActiveBan(targetUUID);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return punishmentDAO.getActiveBan(targetUUID)
                .thenApply(opt -> {
                    cacheManager.cacheBan(targetUUID, opt);
                    return opt;
                });
    }


    public CompletableFuture<Optional<Punishment>> getActiveMute(UUID targetUUID) {
        Optional<Punishment> cached = cacheManager.getActiveMute(targetUUID);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return punishmentDAO.getActiveMute(targetUUID)
                .thenApply(opt -> {
                    cacheManager.cacheMute(targetUUID, opt);
                    return opt;
                });
    }


    public CompletableFuture<List<Punishment>> getHistory(UUID targetUUID) {
        return punishmentDAO.getHistory(targetUUID);
    }


    public CompletableFuture<Optional<Punishment>> getActiveIpBan(String ipAddress) {
        Optional<Punishment> cached = cacheManager.getActiveIpBan(ipAddress);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return punishmentDAO.getActiveIpBan(ipAddress)
                .thenApply(opt -> {
                    cacheManager.cacheIpBan(ipAddress, opt);
                    return opt;
                });
    }


    public CompletableFuture<Optional<Punishment>> getActiveIpMute(String ipAddress) {
        Optional<Punishment> cached = cacheManager.getActiveIpMute(ipAddress);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return punishmentDAO.getActiveIpMute(ipAddress)
                .thenApply(opt -> {
                    cacheManager.cacheIpMute(ipAddress, opt);
                    return opt;
                });
    }


    private CompletableFuture<Punishment> createIpPunishment(UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID, String staffName,
            PunishmentType type, String reason,
            Instant expiresAt) {
        Punishment punishment = new Punishment(targetUUID, targetName, ipAddress, staffUUID, staffName,
                type, reason, Instant.now(), expiresAt);

        return punishmentDAO.insert(punishment)
                .thenApply(id -> {
                    punishment.setId(id);

                    if (type.isIpBan()) {
                        cacheManager.cacheIpBan(ipAddress, Optional.of(punishment));
                        kickIfOnline(targetUUID, type, reason, expiresAt);
                    } else if (type.isIpMute()) {
                        cacheManager.cacheIpMute(ipAddress, Optional.of(punishment));
                        notifyMute(targetUUID, reason, expiresAt);
                    }

                    plugin.getWebhookManager().logPunishment(punishment);

                    return punishment;
                });
    }


    private CompletableFuture<Punishment> createPunishment(UUID targetUUID, String targetName,
            UUID staffUUID, String staffName,
            PunishmentType type, String reason,
            Instant expiresAt) {
        Punishment punishment = new Punishment(targetUUID, targetName, staffUUID, staffName,
                type, reason, Instant.now(), expiresAt);

        return punishmentDAO.insert(punishment)
                .thenApply(id -> {
                    punishment.setId(id);

                    if (type.isBan()) {
                        cacheManager.cacheBan(targetUUID, Optional.of(punishment));
                        kickIfOnline(targetUUID, type, reason, expiresAt);
                    } else if (type.isMute()) {
                        cacheManager.cacheMute(targetUUID, Optional.of(punishment));
                        notifyMute(targetUUID, reason, expiresAt);
                    }

                    plugin.getWebhookManager().logPunishment(punishment);

                    return punishment;
                });
    }


    private void kickIfOnline(UUID targetUUID, PunishmentType type, String reason, Instant expiresAt) {
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null && target.isOnline()) {
            String kickMessage = buildKickMessage(type, reason, expiresAt);
            plugin.getSchedulerManager().runForEntity(target, () -> {
                target.kick(MessageUtil.toComponent(kickMessage));
            });
        }
    }


    private void notifyMute(UUID targetUUID, String reason, Instant expiresAt) {
        Player target = Bukkit.getPlayer(targetUUID);
        if (target != null && target.isOnline()) {
            String muteMsg = plugin.getConfig().getString("messages.mute.applied",
                    "&c&l⚠ &cYou have been muted!");
            muteMsg = muteMsg.replace("{reason}", reason != null ? reason : "No reason specified");
            muteMsg = muteMsg.replace("{expires}", TimeUtil.formatRemaining(expiresAt));

            target.sendMessage(MessageUtil.toComponent(muteMsg));
        }
    }


    public String buildKickMessage(PunishmentType type, String reason, Instant expiresAt) {
        String template;
        if (type == PunishmentType.KICK) {
            template = plugin.getConfig().getString("messages.kick.screen",
                    "&c&lYou have been kicked!\n\n&7Reason: &f{reason}");
        } else {
            template = plugin.getConfig().getString("messages.ban.screen",
                    "&c&lYou are banned from this server!\n\n" +
                            "&7Reason: &f{reason}\n" +
                            "&7Expires: &f{expires}\n\n" +
                            "&7Appeal at: &ediscord.gg/example");
        }

        return template
                .replace("{reason}", reason != null ? reason : "No reason specified")
                .replace("{expires}", TimeUtil.formatRemaining(expiresAt));
    }
}
