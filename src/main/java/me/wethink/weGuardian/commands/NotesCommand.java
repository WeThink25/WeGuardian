package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
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

public class NotesCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public NotesCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.notes")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /notes <player>"));
            return true;
        }

        String targetName = args[0];

        plugin.getDatabaseManager().getPlayerData(targetName).thenAccept(playerData -> {
            if (playerData == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer not found in database."));
                return;
            }

            plugin.getDatabaseManager().getPunishmentsByType(playerData.getUuid(), PunishmentType.NOTE).thenAccept(notes -> {
                if (notes.isEmpty()) {
                    sender.sendMessage(MessageUtils.colorize("&e" + targetName + " has no staff notes."));
                    return;
                }

                sender.sendMessage(MessageUtils.colorize("&6&l=== Staff Notes for " + targetName + " ==="));
                sender.sendMessage(MessageUtils.colorize("&7Total notes: &f" + notes.size()));
                sender.sendMessage("");

                for (Punishment note : notes) {
                    sender.sendMessage(MessageUtils.colorize(
                            "&f#" + note.getId() + " &7by &f" + note.getStaffName() +
                                    " &8(" + TimeUtils.formatDateTime(note.getCreatedAt()) + ")"
                    ));
                    sender.sendMessage(MessageUtils.colorize("&7  Note: &f" + note.getReason()));
                    sender.sendMessage("");
                }

                sender.sendMessage(MessageUtils.colorize("&7Use &f/punish " + targetName + " &7to add/edit/remove notes via GUI"));
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
