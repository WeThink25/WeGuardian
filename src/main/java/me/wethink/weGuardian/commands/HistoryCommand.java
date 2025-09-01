package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HistoryCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public HistoryCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.history")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /history <player>"));
            return true;
        }

        String targetName = args[0];

        plugin.getDatabaseManager().getPlayerData(targetName).thenAccept(playerData -> {
            if (playerData == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer not found in database."));
                return;
            }

            plugin.getDatabaseManager().getPunishmentHistory(playerData.getUuid()).thenAccept(punishments -> {
                if (punishments.isEmpty()) {
                    sender.sendMessage(MessageUtils.colorize("&e" + targetName + " has no punishment history."));
                    return;
                }

                sender.sendMessage(MessageUtils.colorize("&6&l=== Punishment History for " + targetName + " ==="));
                sender.sendMessage(MessageUtils.colorize("&7Total punishments: &f" + punishments.size()));
                sender.sendMessage("");

                for (Punishment punishment : punishments) {
                    String status = punishment.isActive() ? "&a[ACTIVE]" : "&7[EXPIRED]";
                    String timeInfo = punishment.getExpiresAt() != null ?
                            " &8(expires: " + TimeUtils.formatDateTime(punishment.getExpiresAt()) + ")" : " &8(permanent)";

                    sender.sendMessage(MessageUtils.colorize(
                            "&f#" + punishment.getId() + " " + status + " &6" + punishment.getType().name() +
                                    " &7by &f" + punishment.getStaffName() + timeInfo
                    ));
                    sender.sendMessage(MessageUtils.colorize("&7  Reason: &f" + punishment.getReason()));
                    sender.sendMessage(MessageUtils.colorize("&7  Date: &f" + TimeUtils.formatDateTime(punishment.getCreatedAt())));
                    sender.sendMessage("");
                }
            });
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
