package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RollbackCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public RollbackCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.rollback")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /rollback <punishment_id>"));
            return true;
        }

        int punishmentId;
        try {
            punishmentId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid punishment ID. Must be a number."));
            return true;
        }

        final UUID staffUuid;
        final String staffName;

        if (sender instanceof Player player) {
            staffUuid = player.getUniqueId();
            staffName = player.getName();
        } else {
            staffUuid = null;
            staffName = "Console";
        }

        plugin.getDatabaseManager().getPunishmentById(punishmentId).thenAccept(punishment -> {
            if (punishment == null) {
                sender.sendMessage(MessageUtils.colorize("&c✗ Punishment #" + punishmentId + " not found."));
                return;
            }

            if (!punishment.isActive()) {
                sender.sendMessage(MessageUtils.colorize("&cPunishment #" + punishmentId + " is already inactive/expired."));
                return;
            }

            String reason = "Rolled back by " + staffName;

            plugin.getDatabaseManager().removePunishment(punishmentId, staffUuid, staffName, reason).thenAccept(result -> {
                sender.sendMessage(MessageUtils.colorize("&a✓ Successfully rolled back punishment #" + punishmentId));
                sender.sendMessage(MessageUtils.colorize("&7  Type: &f" + punishment.getType().name()));
                sender.sendMessage(MessageUtils.colorize("&7  Target: &f" + punishment.getTargetName()));
                sender.sendMessage(MessageUtils.colorize("&7  Original Staff: &f" + punishment.getStaffName()));
                sender.sendMessage(MessageUtils.colorize("&7  Original Reason: &f" + punishment.getReason()));

                String actionText = switch (punishment.getType()) {
                    case BAN -> "unbanned";
                    case TEMPBAN -> "unbanned";
                    case MUTE -> "unmuted";
                    case TEMPMUTE -> "unmuted";
                    case WARN -> "removed warning for";
                    case KICK -> "reversed kick for";
                    case IPBAN -> "IP unbanned";
                    case IPMUTE -> "IP unmuted";
                    case IPWARN -> "removed IP warning for";
                    case IPKICK -> "reversed IP kick for";
                    case UNBANIP -> "reversed IP unban for";
                    case UNMUTEIP -> "reversed IP unmute for";
                    default -> "reversed punishment for";
                };

                plugin.getNotificationService().broadcastUnpunishment(punishment, staffName, actionText);

                plugin.getDatabaseManager().getPlayerData(punishment.getTargetUuid()).thenAccept(playerData -> {
                    if (playerData != null) {
                        switch (punishment.getType()) {
                            case BAN, TEMPBAN, IPBAN, IPTEMPBAN -> playerData.setBanned(false);
                            case MUTE, TEMPMUTE, IPMUTE, IPTEMPMUTE -> playerData.setMuted(false);
                            case UNBAN -> playerData.setBanned(false);
                            case UNMUTE -> playerData.setMuted(false);
                            case KICK, IPKICK -> playerData.setKicked(false);
                            case NOTE -> playerData.setNoted(false);
                            case WARN -> playerData.setWarned(false);
                            case IPWARN -> playerData.setIpWarned(false);
                            case UNBANIP -> playerData.setIpBanned(false);
                            case UNMUTEIP -> playerData.setIpMuted(false);
                        }
                        plugin.getDatabaseManager().savePlayerData(playerData);
                    }
                });
            }).exceptionally(ex -> {
                sender.sendMessage(MessageUtils.colorize("&c✗ Failed to rollback punishment: " + ex.getMessage()));
                return null;
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
