package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnwarnCommand implements CommandExecutor {

    private final WeGuardian plugin;

    public UnwarnCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.unwarn")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /unwarn <warning_id> [reason]"));
            return true;
        }

        int warningId;
        try {
            warningId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid warning ID: " + args[0]));
            return true;
        }

        String reason = "No reason provided";
        if (args.length > 1) {
            reason = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        }

        final String finalReason = reason;
        String staffName = sender.getName();
        UUID staffUuid = sender instanceof org.bukkit.entity.Player ?
                ((org.bukkit.entity.Player) sender).getUniqueId() :
                UUID.fromString("00000000-0000-0000-0000-000000000000");

        plugin.getDatabaseManager().getPunishmentById(warningId).thenAccept(punishment -> {
            if (punishment == null) {
                sender.sendMessage(MessageUtils.colorize("&cNo warning found with ID: " + warningId));
                return;
            }

            if (punishment.getType() != PunishmentType.WARN) {
                sender.sendMessage(MessageUtils.colorize("&cPunishment ID " + warningId + " is not a warning (it's a " +
                        punishment.getType().name().toLowerCase() + ")"));
                return;
            }

            if (!punishment.isActive()) {
                sender.sendMessage(MessageUtils.colorize("&cWarning ID " + warningId + " is already removed."));
                return;
            }

            if (!plugin.getPunishmentService().canPunish(sender, punishment.getTargetName()).join()) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot remove warnings for this player due to permission hierarchy."));
                return;
            }

            plugin.getDatabaseManager().removePunishment(warningId, staffUuid, staffName, finalReason)
                    .thenRun(() -> {
                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully removed warning ID " + warningId +
                                " for player " + punishment.getTargetName()));

                        String broadcastMessage = MessageUtils.colorize("&7[&cUnwarn&7] &f" + staffName +
                                " &7removed warning &e#" + warningId + " &7for &f" + punishment.getTargetName() +
                                " &8(&7" + finalReason + "&8)");

                        plugin.getServer().getOnlinePlayers().stream()
                                .filter(player -> player.hasPermission("weguardian.notify"))
                                .forEach(player -> player.sendMessage(broadcastMessage));

                        plugin.getLogger().info(staffName + " removed warning #" + warningId + " for " + punishment.getTargetName());
                    })
                    .exceptionally(throwable -> {
                        sender.sendMessage(MessageUtils.colorize("&cError removing warning: " + throwable.getMessage()));
                        plugin.getLogger().severe("Error in unwarn command: " + throwable.getMessage());
                        return null;
                    });

        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError retrieving warning: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in unwarn command: " + throwable.getMessage());
            return null;
        });

        return true;
    }


}
