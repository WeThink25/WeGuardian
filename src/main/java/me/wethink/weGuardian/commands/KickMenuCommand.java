package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
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

public class KickMenuCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public KickMenuCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(MessageUtils.colorize("&cThis command can only be used by players."));
            return true;
        }

        Player staff = (Player) sender;

        if (!staff.hasPermission("weguardian.kick")) {
            staff.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            staff.sendMessage(MessageUtils.colorize("&cUsage: /kickmenu <player>"));
            return true;
        }

        String targetPlayerName = args[0];

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            staff.sendMessage(MessageUtils.colorize("&cPlayer '" + targetPlayerName + "' is not online."));
            return true;
        }

        targetPlayerName = targetPlayer.getName();

        if (staff.getName().equalsIgnoreCase(targetPlayerName)) {
            staff.sendMessage(MessageUtils.colorize("&cYou cannot kick yourself!"));
            return true;
        }

        if (targetPlayer.hasPermission("weguardian.bypass")) {
            staff.sendMessage(MessageUtils.colorize("&cYou cannot kick this player!"));
            return true;
        }

        plugin.getPunishmentGUI().getMenuManager().openMenu(staff, targetPlayerName, "kick");
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            String input = args[0].toLowerCase();
            completions = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }

        return completions;
    }
}
