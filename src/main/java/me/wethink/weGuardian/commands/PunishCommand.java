package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.PunishmentGUI;
import me.wethink.weGuardian.utils.MessageUtils;
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

public class PunishCommand implements CommandExecutor {

    private final WeGuardian plugin;
    private final PunishmentGUI punishmentGUI;

    public PunishCommand(WeGuardian plugin, PunishmentGUI punishmentGUI) {
        this.plugin = plugin;
        this.punishmentGUI = punishmentGUI;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize("&cThis command can only be used by players."));
            return true;
        }

        Player staff = (Player) sender;

        if (!staff.hasPermission("weguardian.punish")) {
            staff.sendMessage(MessageUtils.colorize(
                    plugin.getConfig().getString("messages.commands.no_permission",
                            "&cYou don't have permission to use this command.")
            ));
            return true;
        }

        if (args.length != 1) {
            staff.sendMessage(MessageUtils.colorize("&cUsage: /punish <player>"));
            return true;
        }

        String targetPlayerName = args[0];

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            if (Bukkit.getOfflinePlayer(targetPlayerName).hasPlayedBefore()) {
                targetPlayerName = Bukkit.getOfflinePlayer(targetPlayerName).getName();
            } else {
                staff.sendMessage(MessageUtils.colorize(
                        plugin.getConfig().getString("messages.commands.player_not_found",
                                        "&cPlayer '%player%' not found.")
                                .replace("%player%", targetPlayerName)
                ));
                return true;
            }
        } else {
            targetPlayerName = targetPlayer.getName();
        }

        if (staff.getName().equalsIgnoreCase(targetPlayerName)) {
            staff.sendMessage(MessageUtils.colorize("&cYou cannot punish yourself!"));
            return true;
        }

        if (targetPlayer != null && targetPlayer.hasPermission("weguardian.bypass")) {
            staff.sendMessage(MessageUtils.colorize("&cYou cannot punish this player!"));
            return true;
        }

        punishmentGUI.openMainGUI(staff, targetPlayerName);

        return true;
    }


}
