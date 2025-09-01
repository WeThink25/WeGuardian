package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class BlameCommand implements CommandExecutor, TabCompleter {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final WeGuardian plugin;

    public BlameCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.blame")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /blame <punishment_id>"));
            return true;
        }

        int punishmentId;
        try {
            punishmentId = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid punishment ID: " + args[0]));
            return true;
        }

        plugin.getDatabaseManager().getPunishmentById(punishmentId).thenAccept(punishment -> {
            if (punishment == null) {
                sender.sendMessage(MessageUtils.colorize("&cNo punishment found with ID: " + punishmentId));
                return;
            }

            displayPunishmentDetails(sender, punishment);
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError retrieving punishment: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in blame command: " + throwable.getMessage());
            return null;
        });

        return true;
    }

    private void displayPunishmentDetails(CommandSender sender, Punishment punishment) {
        sender.sendMessage(MessageUtils.colorize("&6&l=== Punishment Details (ID: " + punishment.getId() + ") ==="));
        sender.sendMessage("");

        sender.sendMessage(MessageUtils.colorize("&7Type: &f" + punishment.getType().name()));

        String target = punishment.isIPBased() ? punishment.getTargetIP() : punishment.getTargetName();
        sender.sendMessage(MessageUtils.colorize("&7Target: &f" + target +
                (punishment.isIPBased() ? " &8(IP)" : " &8(Player)")));

        sender.sendMessage(MessageUtils.colorize("&7Staff: &f" + punishment.getStaffName()));
        sender.sendMessage(MessageUtils.colorize("&7Reason: &f" + punishment.getReason()));
        sender.sendMessage(MessageUtils.colorize("&7Date: &f" + punishment.getCreatedAt().format(DATE_FORMAT)));

        String status = punishment.isActive() ? "&aActive" : "&cInactive";
        if (punishment.isActive() && punishment.isExpired()) {
            status = "&eExpired";
        }
        sender.sendMessage(MessageUtils.colorize("&7Status: " + status));

        if (punishment.getExpiresAt() != null) {
            sender.sendMessage(MessageUtils.colorize("&7Expires: &f" + punishment.getExpiresAt().format(DATE_FORMAT)));

            if (punishment.isActive() && !punishment.isExpired()) {
                long timeLeft = java.time.Duration.between(java.time.LocalDateTime.now(), punishment.getExpiresAt()).toMillis();
                sender.sendMessage(MessageUtils.colorize("&7Time Left: &f" + TimeUtils.formatDuration(timeLeft)));
            }
        } else {
            sender.sendMessage(MessageUtils.colorize("&7Duration: &fPermanent"));
        }

        if (!punishment.isActive() && punishment.getRemovedAt() != null) {
            sender.sendMessage("");
            sender.sendMessage(MessageUtils.colorize("&c&lRemoval Information:"));
            sender.sendMessage(MessageUtils.colorize("&7Removed by: &f" + punishment.getRemovedByName()));
            sender.sendMessage(MessageUtils.colorize("&7Removed at: &f" + punishment.getRemovedAt().format(DATE_FORMAT)));
            if (punishment.getRemovalReason() != null) {
                sender.sendMessage(MessageUtils.colorize("&7Removal reason: &f" + punishment.getRemovalReason()));
            }
        }

        sender.sendMessage("");
        sender.sendMessage(MessageUtils.colorize("&7UUID: &8" + punishment.getTargetUuid()));
        if (punishment.getStaffUuid() != null) {
            sender.sendMessage(MessageUtils.colorize("&7Staff UUID: &8" + punishment.getStaffUuid()));
        }

        sender.sendMessage("");
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.add("<punishment_id>");
        }

        return completions;
    }
}
