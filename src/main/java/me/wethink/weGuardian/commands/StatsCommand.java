package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatsCommand implements CommandExecutor {

    private final WeGuardian plugin;

    public StatsCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.stats")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        plugin.getDatabaseManager().getAllPunishments().thenAccept(punishments -> {
            if (punishments.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&eNo punishments found in the database."));
                return;
            }

            Map<PunishmentType, Integer> totalStats = new HashMap<>();
            Map<PunishmentType, Integer> activeStats = new HashMap<>();

            for (PunishmentType type : PunishmentType.values()) {
                totalStats.put(type, 0);
                activeStats.put(type, 0);
            }

            for (Punishment punishment : punishments) {
                PunishmentType type = punishment.getType();
                totalStats.put(type, totalStats.get(type) + 1);

                if (punishment.isActive()) {
                    activeStats.put(type, activeStats.get(type) + 1);
                }
            }

            int totalPunishments = punishments.size();
            int activePunishments = punishments.stream()
                    .mapToInt(p -> p.isActive() ? 1 : 0)
                    .sum();

            sender.sendMessage(MessageUtils.colorize("&6&l=== WeGuardian Global Statistics ==="));
            sender.sendMessage(MessageUtils.colorize("&7Total punishments: &f" + totalPunishments));
            sender.sendMessage(MessageUtils.colorize("&7Active punishments: &a" + activePunishments));
            sender.sendMessage(MessageUtils.colorize("&7Expired/removed: &7" + (totalPunishments - activePunishments)));
            sender.sendMessage("");

            sender.sendMessage(MessageUtils.colorize("&6Total Punishment Breakdown:"));
            sender.sendMessage(MessageUtils.colorize("&7  Bans: &f" + totalStats.get(PunishmentType.BAN) + " &8(Active: &a" + activeStats.get(PunishmentType.BAN) + "&8)"));
            sender.sendMessage(MessageUtils.colorize("&7  Tempbans: &f" + totalStats.get(PunishmentType.TEMPBAN) + " &8(Active: &a" + activeStats.get(PunishmentType.TEMPBAN) + "&8)"));
            sender.sendMessage(MessageUtils.colorize("&7  Mutes: &f" + totalStats.get(PunishmentType.MUTE) + " &8(Active: &a" + activeStats.get(PunishmentType.MUTE) + "&8)"));
            sender.sendMessage(MessageUtils.colorize("&7  Tempmutes: &f" + totalStats.get(PunishmentType.TEMPMUTE) + " &8(Active: &a" + activeStats.get(PunishmentType.TEMPMUTE) + "&8)"));
            sender.sendMessage(MessageUtils.colorize("&7  Kicks: &f" + totalStats.get(PunishmentType.KICK)));
            sender.sendMessage(MessageUtils.colorize("&7  Warns: &f" + totalStats.get(PunishmentType.WARN)));
            sender.sendMessage(MessageUtils.colorize("&7  Notes: &f" + totalStats.get(PunishmentType.NOTE)));
            sender.sendMessage("");

            long uniquePlayersCount = punishments.stream()
                    .map(Punishment::getTargetUuid)
                    .distinct()
                    .count();

            long uniqueStaffCount = punishments.stream()
                    .map(Punishment::getStaffUuid)
                    .distinct()
                    .count();

            sender.sendMessage(MessageUtils.colorize("&7Unique punished players: &f" + uniquePlayersCount));
            sender.sendMessage(MessageUtils.colorize("&7Active staff members: &f" + uniqueStaffCount));
        });

        return true;
    }


}
