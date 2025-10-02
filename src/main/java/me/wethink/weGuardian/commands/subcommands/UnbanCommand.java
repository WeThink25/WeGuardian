package me.wethink.weGuardian.commands.subcommands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final PunishmentService punishmentService;

    public UnbanCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.unban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /unban <player>"));
            return true;
        }

        String targetName = args[0];

        String staffName = sender instanceof Player ? sender.getName() : "Console";

        UUID targetUuid = Bukkit.getOfflinePlayer(targetName).getUniqueId();

        punishmentService.unbanPlayer(targetUuid, targetName, staffName)
                .thenAccept(success -> {
                    String message = success ?
                            "&aSuccessfully unbanned " + targetName :
                            "&c" + targetName + " is not currently banned.";
                    sender.sendMessage(MessageUtils.colorize(message));
                });

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();

            suggestions.addAll(Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList()));

            suggestions.addAll(Arrays.stream(Bukkit.getOfflinePlayers())
                    .map(org.bukkit.OfflinePlayer::getName)
                    .filter(name -> name != null && name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList()));

            return suggestions;
        }
        return new ArrayList<>();
    }
}