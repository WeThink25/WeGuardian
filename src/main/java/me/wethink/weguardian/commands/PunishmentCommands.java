package me.wethink.weguardian.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.gui.PunishmentGUI;
import me.wethink.weguardian.gui.HistoryGUI;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

@CommandAlias("weguardian|wg")
public class PunishmentCommands extends BaseCommand {

    private static final String PREFIX = "&8[&cWeGuardian&8] ";
    private static final String MSG_PLAYER_NOT_FOUND = "&cPlayer not found!";
    private static final String MSG_PLAYER_NOT_ONLINE = "&cPlayer is not online!";
    private static final String MSG_PLAYER_BYPASS = "&cYou cannot punish this player!";
    private static final String MSG_INVALID_DURATION = "&cInvalid duration format! Use: 1h, 30m, 7d, etc.";
    private static final String MSG_PLAYER_MUST_BE_ONLINE_IP = "&cPlayer must be online to resolve their IP!";
    private static final String MSG_COULD_NOT_RESOLVE_IP = "&cCould not resolve player's IP address!";

    private final WeGuardian plugin;

    public PunishmentCommands(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @CommandAlias("ban")
    @CommandPermission("weguardian.ban")
    @CommandCompletion("@players")
    @Description("Permanently ban a player")
    @Syntax("<player> [reason]")
    public void onBan(CommandSender sender, String targetName, @Optional String reason) {
        executePunishment(sender, targetName, PunishmentType.BAN, -1, reason);
    }

    @CommandAlias("tempban")
    @CommandPermission("weguardian.tempban")
    @CommandCompletion("@players")
    @Description("Temporarily ban a player")
    @Syntax("<player> <duration> [reason]")
    public void onTempBan(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0) {
            sender.sendMessage(MessageUtil.toComponent(MSG_INVALID_DURATION));
            return;
        }
        executePunishment(sender, targetName, PunishmentType.TEMPBAN, durationMs, reason);
    }

    @CommandAlias("unban")
    @CommandPermission("weguardian.unban")
    @CommandCompletion("@players")
    @Description("Unban a player")
    @Syntax("<player>")
    public void onUnban(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
            return;
        }

        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        plugin.getPunishmentManager().unban(target.getUniqueId(), staffUUID, staffName, "Unbanned")
                .thenAccept(success -> {
                    if (success) {
                        broadcastStaff("&a" + staffName + " &7unbanned &a" + targetName);
                        sender.sendMessage(MessageUtil.toComponent("&aSuccessfully unbanned " + targetName));
                    } else {
                        sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " is not banned!"));
                    }
                });
    }

    @CommandAlias("mute")
    @CommandPermission("weguardian.mute")
    @CommandCompletion("@players")
    @Description("Permanently mute a player")
    @Syntax("<player> [reason]")
    public void onMute(CommandSender sender, String targetName, @Optional String reason) {
        executePunishment(sender, targetName, PunishmentType.MUTE, -1, reason);
    }

    @CommandAlias("tempmute")
    @CommandPermission("weguardian.tempmute")
    @CommandCompletion("@players")
    @Description("Temporarily mute a player")
    @Syntax("<player> <duration> [reason]")
    public void onTempMute(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0) {
            sender.sendMessage(MessageUtil.toComponent(MSG_INVALID_DURATION));
            return;
        }
        executePunishment(sender, targetName, PunishmentType.TEMPMUTE, durationMs, reason);
    }

    @CommandAlias("unmute")
    @CommandPermission("weguardian.unmute")
    @CommandCompletion("@players")
    @Description("Unmute a player")
    @Syntax("<player>")
    public void onUnmute(CommandSender sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
            return;
        }

        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        plugin.getPunishmentManager().unmute(target.getUniqueId(), staffUUID, staffName, "Unmuted")
                .thenAccept(success -> {
                    if (success) {
                        broadcastStaff("&a" + staffName + " &7unmuted &a" + targetName);
                        sender.sendMessage(MessageUtil.toComponent("&aSuccessfully unmuted " + targetName));

                        Player onlineTarget = Bukkit.getPlayer(target.getUniqueId());
                        if (onlineTarget != null) {
                            plugin.getSchedulerManager().runForEntity(onlineTarget, () -> onlineTarget
                                    .sendMessage(MessageUtil.toComponent("&aYou have been unmuted!")));
                        }
                    } else {
                        sender.sendMessage(MessageUtil.toComponent("&c" + targetName + " is not muted!"));
                    }
                });
    }

    @CommandAlias("banip")
    @CommandPermission("weguardian.banip")
    @CommandCompletion("@players")
    @Description("Permanently IP ban a player (auto-resolves IP)")
    @Syntax("<player> [reason]")
    public void onBanIp(CommandSender sender, String targetName, @Optional String reason) {
        executeIpPunishment(sender, targetName, PunishmentType.BANIP, -1, reason);
    }

    @CommandAlias("tempbanip")
    @CommandPermission("weguardian.tempbanip")
    @CommandCompletion("@players")
    @Description("Temporarily IP ban a player (auto-resolves IP)")
    @Syntax("<player> <duration> [reason]")
    public void onTempBanIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0) {
            sender.sendMessage(MessageUtil.toComponent(MSG_INVALID_DURATION));
            return;
        }
        executeIpPunishment(sender, targetName, PunishmentType.TEMPBANIP, durationMs, reason);
    }

    @CommandAlias("unbanip")
    @CommandPermission("weguardian.unbanip")
    @CommandCompletion("@players")
    @Description("Remove an IP ban for a player (auto-resolves IP)")
    @Syntax("<player>")
    public void onUnbanIp(CommandSender sender, String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        if (onlineTarget != null) {
            String ipAddress = getPlayerIp(onlineTarget);
            if (ipAddress == null) {
                sender.sendMessage(MessageUtil.toComponent(MSG_COULD_NOT_RESOLVE_IP));
                return;
            }
            executeUnbanIp(sender, onlineTarget.getUniqueId(), targetName, ipAddress, staffUUID, staffName);
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
                return;
            }

            plugin.getPunishmentDAO().getPlayerIp(offlineTarget.getUniqueId()).thenAccept(optionalIp -> {
                if (optionalIp.isEmpty()) {
                    sender.sendMessage(MessageUtil.toComponent("&cNo stored IP found for " + targetName + "!"));
                    return;
                }
                executeUnbanIp(sender, offlineTarget.getUniqueId(), targetName, optionalIp.get(), staffUUID, staffName);
            });
        }
    }

    private void executeUnbanIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID,
            String staffName) {
        plugin.getPunishmentManager().unbanIp(targetUUID, ipAddress, staffUUID, staffName, "Unbanned")
                .thenAccept(success -> {
                    if (success) {
                        broadcastStaff(
                                "&a" + staffName + " &7removed IP ban for &a" + targetName + " &7(" + ipAddress + ")");
                        sender.sendMessage(MessageUtil.toComponent("&aSuccessfully removed IP ban for " + targetName));
                    } else {
                        sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not banned!"));
                    }
                });
    }

    @CommandAlias("muteip")
    @CommandPermission("weguardian.muteip")
    @CommandCompletion("@players")
    @Description("Permanently IP mute a player (auto-resolves IP)")
    @Syntax("<player> [reason]")
    public void onMuteIp(CommandSender sender, String targetName, @Optional String reason) {
        executeIpPunishment(sender, targetName, PunishmentType.MUTEIP, -1, reason);
    }

    @CommandAlias("tempmuteip")
    @CommandPermission("weguardian.tempmuteip")
    @CommandCompletion("@players")
    @Description("Temporarily IP mute a player (auto-resolves IP)")
    @Syntax("<player> <duration> [reason]")
    public void onTempMuteIp(CommandSender sender, String targetName, String duration, @Optional String reason) {
        long durationMs = TimeUtil.parseDuration(duration);
        if (durationMs <= 0) {
            sender.sendMessage(MessageUtil.toComponent(MSG_INVALID_DURATION));
            return;
        }
        executeIpPunishment(sender, targetName, PunishmentType.TEMPMUTEIP, durationMs, reason);
    }

    @CommandAlias("unmuteip")
    @CommandPermission("weguardian.unmuteip")
    @CommandCompletion("@players")
    @Description("Remove an IP mute for a player (auto-resolves IP)")
    @Syntax("<player>")
    public void onUnmuteIp(CommandSender sender, String targetName) {
        Player onlineTarget = Bukkit.getPlayer(targetName);
        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();

        if (onlineTarget != null) {
            String ipAddress = getPlayerIp(onlineTarget);
            if (ipAddress == null) {
                sender.sendMessage(MessageUtil.toComponent(MSG_COULD_NOT_RESOLVE_IP));
                return;
            }
            executeUnmuteIp(sender, onlineTarget.getUniqueId(), targetName, ipAddress, staffUUID, staffName);
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
                return;
            }

            plugin.getPunishmentDAO().getPlayerIp(offlineTarget.getUniqueId()).thenAccept(optionalIp -> {
                if (optionalIp.isEmpty()) {
                    sender.sendMessage(MessageUtil.toComponent("&cNo stored IP found for " + targetName + "!"));
                    return;
                }
                executeUnmuteIp(sender, offlineTarget.getUniqueId(), targetName, optionalIp.get(), staffUUID,
                        staffName);
            });
        }
    }

    private void executeUnmuteIp(CommandSender sender, UUID targetUUID, String targetName, String ipAddress,
            UUID staffUUID,
            String staffName) {
        plugin.getPunishmentManager().unmuteIp(targetUUID, ipAddress, staffUUID, staffName, "Unmuted")
                .thenAccept(success -> {
                    if (success) {
                        broadcastStaff(
                                "&a" + staffName + " &7removed IP mute for &a" + targetName + " &7(" + ipAddress + ")");
                        sender.sendMessage(MessageUtil.toComponent("&aSuccessfully removed IP mute for " + targetName));
                    } else {
                        sender.sendMessage(MessageUtil.toComponent("&c" + targetName + "'s IP is not muted!"));
                    }
                });
    }

    @CommandAlias("kick")
    @CommandPermission("weguardian.kick")
    @CommandCompletion("@players")
    @Description("Kick a player from the server")
    @Syntax("<player> [reason]")
    public void onKick(CommandSender sender, String targetName, @Optional String reason) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_ONLINE));
            return;
        }

        String bypassPerm = plugin.getConfig().getString("bypass.permission", "weguardian.bypass");
        if (target.hasPermission(bypassPerm)) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_BYPASS));
            return;
        }

        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "Kicked by staff";

        plugin.getPunishmentManager().kick(target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason)
                .thenAccept(success -> {
                    if (success) {
                        broadcastStaff("&c" + staffName + " &7kicked &c" + targetName + " &7for: &f" + finalReason);
                        sender.sendMessage(MessageUtil.toComponent("&aSuccessfully kicked " + targetName));
                    } else {
                        sender.sendMessage(MessageUtil.toComponent("&cFailed to kick " + targetName));
                    }
                });
    }

    @CommandAlias("punish")
    @CommandPermission("weguardian.punish")
    @CommandCompletion("@players")
    @Description("Open the punishment GUI for a player")
    @Syntax("<player>")
    public void onPunish(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
            return;
        }

        PunishmentGUI.openAsync(plugin, sender, target);
    }

    @CommandAlias("history")
    @CommandPermission("weguardian.history")
    @CommandCompletion("@players")
    @Description("View punishment history for a player")
    @Syntax("<player>")
    public void onHistory(Player sender, String targetName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
            return;
        }

        HistoryGUI.openAsync(plugin, sender, target);
    }

    @Subcommand("reload")
    @CommandPermission("weguardian.admin")
    @Description("Reload the plugin configuration")
    public void onReload(CommandSender sender) {
        plugin.reloadConfig();
        plugin.getCacheManager().clear();
        sender.sendMessage(MessageUtil.toComponent("&aWeGuardian configuration reloaded!"));
    }

    @Subcommand("help")
    @CommandAlias("wghelp")
    @CommandPermission("weguardian.help")
    @Description("Show all WeGuardian commands")
    public void onHelp(CommandSender sender) {
        String[] helpLines = {
                "",
                "&8&m━━━━━━━━━━━━━━━━━━━━&r &c&lWeGuardian Help &8&m━━━━━━━━━━━━━━━━━━━━",
                "",
                "&c/ban &7<player> [reason] &8- &fPermanently ban a player",
                "&c/tempban &7<player> <duration> [reason] &8- &fTemporarily ban",
                "&c/unban &7<player> &8- &fUnban a player",
                "",
                "&e/mute &7<player> [reason] &8- &fPermanently mute a player",
                "&e/tempmute &7<player> <duration> [reason] &8- &fTemporarily mute",
                "&e/unmute &7<player> &8- &fUnmute a player",
                "",
                "&d/banip &7<player> [reason] &8- &fIP ban a player",
                "&d/tempbanip &7<player> <duration> [reason] &8- &fTemp IP ban",
                "&d/unbanip &7<player> &8- &fRemove IP ban",
                "",
                "&b/muteip &7<player> [reason] &8- &fIP mute a player",
                "&b/tempmuteip &7<player> <duration> [reason] &8- &fTemp IP mute",
                "&b/unmuteip &7<player> &8- &fRemove IP mute",
                "",
                "&6/kick &7<player> [reason] &8- &fKick a player",
                "&a/punish &7<player> &8- &fOpen punishment GUI",
                "&a/history &7<player> &8- &fView punishment history",
                "",
                "&c/wg reload &8- &fReload configuration",
                "&c/wg help &8- &fShow this help menu",
                "",
                "&7Duration formats: &f1s, 30m, 6h, 7d, 4w, 1M, 1y",
                "&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
                ""
        };

        for (String line : helpLines) {
            sender.sendMessage(MessageUtil.toComponent(line));
        }
    }

    private void executePunishment(CommandSender sender, String targetName, PunishmentType type,
            long durationMs, String reason) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (!target.hasPlayedBefore() && !target.isOnline()) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
            return;
        }

        Player onlineTarget = target.getPlayer();
        String bypassPerm = plugin.getConfig().getString("bypass.permission", "weguardian.bypass");
        if (onlineTarget != null && onlineTarget.hasPermission(bypassPerm)) {
            sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_BYPASS));
            return;
        }

        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "No reason specified";

        var future = switch (type) {
            case BAN -> plugin.getPunishmentManager().ban(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason);
            case TEMPBAN -> plugin.getPunishmentManager().tempban(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, finalReason);
            case MUTE -> plugin.getPunishmentManager().mute(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, finalReason);
            case TEMPMUTE -> plugin.getPunishmentManager().tempmute(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, finalReason);
            default -> null;
        };

        if (future == null)
            return;

        future.thenAccept(punishment -> {
            String durationStr = durationMs > 0 ? TimeUtil.formatDuration(durationMs) : "permanent";
            String actionVerb = type.getDisplayName().toLowerCase() + "ned";
            broadcastStaff("&c" + staffName + " &7" + actionVerb + " &c" + targetName + " &7for &f" + finalReason
                    + " &7(" + durationStr + ")");
            sender.sendMessage(MessageUtil
                    .toComponent("&aSuccessfully " + actionVerb + " " + targetName + " (" + durationStr + ")"));
        }).exceptionally(e -> {
            sender.sendMessage(MessageUtil.toComponent("&cAn error occurred while applying the punishment."));
            plugin.getLogger().severe("Failed to apply punishment: " + e.getMessage());
            return null;
        });
    }

    private void broadcastStaff(String message) {
        String formattedMessage = PREFIX + message;
        Collection<? extends Player> players = Bukkit.getOnlinePlayers();

        for (Player player : players) {
            if (player.hasPermission("weguardian.staff")) {
                player.sendMessage(MessageUtil.toComponent(formattedMessage));
            }
        }
        Bukkit.getConsoleSender().sendMessage(MessageUtil.toComponent(formattedMessage));
    }

    private void executeIpPunishment(CommandSender sender, String targetName, PunishmentType type,
            long durationMs, String reason) {
        Player onlineTarget = Bukkit.getPlayer(targetName);

        if (onlineTarget != null) {
            String bypassPerm = plugin.getConfig().getString("bypass.permission", "weguardian.bypass");
            if (onlineTarget.hasPermission(bypassPerm)) {
                sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_BYPASS));
                return;
            }

            String ipAddress = getPlayerIp(onlineTarget);
            if (ipAddress == null) {
                sender.sendMessage(MessageUtil.toComponent(MSG_COULD_NOT_RESOLVE_IP));
                return;
            }

            applyIpPunishment(sender, onlineTarget.getUniqueId(), targetName, ipAddress, type, durationMs, reason);
        } else {
            OfflinePlayer offlineTarget = Bukkit.getOfflinePlayer(targetName);
            if (!offlineTarget.hasPlayedBefore()) {
                sender.sendMessage(MessageUtil.toComponent(MSG_PLAYER_NOT_FOUND));
                return;
            }

            plugin.getPunishmentDAO().getPlayerIp(offlineTarget.getUniqueId()).thenAccept(optionalIp -> {
                if (optionalIp.isEmpty()) {
                    sender.sendMessage(MessageUtil.toComponent("&cNo stored IP found for " + targetName + "!"));
                    return;
                }

                String ipAddress = optionalIp.get();
                applyIpPunishment(sender, offlineTarget.getUniqueId(), targetName, ipAddress, type, durationMs, reason);
            });
        }
    }

    private void applyIpPunishment(CommandSender sender, UUID targetUUID, String targetName, String ipAddress,
            PunishmentType type, long durationMs, String reason) {
        UUID staffUUID = sender instanceof Player p ? p.getUniqueId() : null;
        String staffName = sender.getName();
        String finalReason = reason != null ? reason : "No reason specified";

        var future = switch (type) {
            case BANIP -> plugin.getPunishmentManager().banIp(
                    targetUUID, targetName, ipAddress, staffUUID, staffName, finalReason);
            case TEMPBANIP -> plugin.getPunishmentManager().tempbanIp(
                    targetUUID, targetName, ipAddress, staffUUID, staffName, durationMs, finalReason);
            case MUTEIP -> plugin.getPunishmentManager().muteIp(
                    targetUUID, targetName, ipAddress, staffUUID, staffName, finalReason);
            case TEMPMUTEIP -> plugin.getPunishmentManager().tempmuteIp(
                    targetUUID, targetName, ipAddress, staffUUID, staffName, durationMs, finalReason);
            default -> null;
        };

        if (future == null)
            return;

        future.thenAccept(punishment -> {
            String durationStr = durationMs > 0 ? TimeUtil.formatDuration(durationMs) : "permanent";
            String actionVerb = type.getDisplayName().toLowerCase() + "ned";
            broadcastStaff("&c" + staffName + " &7" + actionVerb + " &c" + targetName + " &7(&f" + ipAddress
                    + "&7) for &f" + finalReason + " &7(" + durationStr + ")");
            sender.sendMessage(MessageUtil.toComponent(
                    "&aSuccessfully " + actionVerb + " " + targetName + " (" + ipAddress + ") (" + durationStr + ")"));
        }).exceptionally(e -> {
            sender.sendMessage(MessageUtil.toComponent("&cAn error occurred while applying the punishment."));
            plugin.getLogger().severe("Failed to apply IP punishment: " + e.getMessage());
            return null;
        });
    }

    private static String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        return address != null ? address.getAddress().getHostAddress() : null;
    }
}
