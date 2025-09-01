package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StaffStatusCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public StaffStatusCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.staffstatus")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /staffstatus <staff>"));
            return true;
        }

        String staffName = args[0];

        plugin.getDatabaseManager().getPunishmentsByStaff(staffName).thenAccept(punishments -> {
            if (punishments.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&e" + staffName + " has not issued any punishments."));
                return;
            }

            Map<PunishmentType, Integer> stats = new HashMap<>();
            for (PunishmentType type : PunishmentType.values()) {
                stats.put(type, 0);
            }

            for (Punishment punishment : punishments) {
                stats.put(punishment.getType(), stats.get(punishment.getType()) + 1);
            }

            sender.sendMessage(MessageUtils.colorize("&6&l=== Staff Statistics for " + staffName + " ==="));
            sender.sendMessage(MessageUtils.colorize("&7Total punishments issued: &f" + punishments.size()));
            sender.sendMessage("");

            sender.sendMessage(MessageUtils.colorize("&6Punishment Breakdown:"));
            sender.sendMessage(MessageUtils.colorize("&7  Bans: &f" + stats.get(PunishmentType.BAN)));
            sender.sendMessage(MessageUtils.colorize("&7  Tempbans: &f" + stats.get(PunishmentType.TEMPBAN)));
            sender.sendMessage(MessageUtils.colorize("&7  Mutes: &f" + stats.get(PunishmentType.MUTE)));
            sender.sendMessage(MessageUtils.colorize("&7  Tempmutes: &f" + stats.get(PunishmentType.TEMPMUTE)));
            sender.sendMessage(MessageUtils.colorize("&7  Kicks: &f" + stats.get(PunishmentType.KICK)));
            sender.sendMessage(MessageUtils.colorize("&7  Warns: &f" + stats.get(PunishmentType.WARN)));
            sender.sendMessage(MessageUtils.colorize("&7  Notes: &f" + stats.get(PunishmentType.NOTE)));
            sender.sendMessage("");

            int activePunishments = (int) punishments.stream()
                    .filter(Punishment::isActive)
                    .count();

            sender.sendMessage(MessageUtils.colorize("&7Active punishments: &f" + activePunishments));
            sender.sendMessage(MessageUtils.colorize("&7Expired/removed: &f" + (punishments.size() - activePunishments)));
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();

            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));

            return suggestions;
        }
        return new ArrayList<>();
    }
}
