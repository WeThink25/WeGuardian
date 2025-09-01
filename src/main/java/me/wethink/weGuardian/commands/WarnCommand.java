package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final PunishmentService punishmentService;

    public WarnCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.warn")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /warn <player> <reason>"));
            return true;
        }

        String targetName = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        Player target = Bukkit.getPlayer(targetName);
        if (target == null && !Bukkit.getOfflinePlayer(targetName).hasPlayedBefore()) {
            sender.sendMessage(MessageUtils.colorize("&cPlayer not found."));
            return true;
        }

        final String finalTargetName;
        if (target != null) {
            finalTargetName = target.getName();
            if (target.hasPermission("weguardian.bypass")) {
                sender.sendMessage(MessageUtils.colorize("&cYou cannot punish this player!"));
                return true;
            }
        } else {
            finalTargetName = targetName;
        }

        String staffName = sender instanceof Player ? sender.getName() : "Console";

        UUID targetUuid = target != null ? target.getUniqueId() :
                          Bukkit.getOfflinePlayer(finalTargetName).getUniqueId();

        punishmentService.warnPlayer(targetUuid, finalTargetName, staffName, reason)
                .thenAccept(success -> {
                    String message = success ?
                            "&aSuccessfully warned " + finalTargetName + " for: &e" + reason :
                            "&cFailed to warn " + finalTargetName + ". Player may not exist.";
                    sender.sendMessage(MessageUtils.colorize(message));
                });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
