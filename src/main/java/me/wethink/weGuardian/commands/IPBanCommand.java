package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IPBanCommand implements CommandExecutor, TabCompleter {

    private final PunishmentService punishmentService;

    public IPBanCommand(PunishmentService punishmentService) {
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.ipban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /ipban <ip> <reason>"));
            return true;
        }

        String ipAddress = args[0];
        String reason = String.join(" ", Arrays.copyOfRange(args, 1, args.length));

        if (!isValidIP(ipAddress)) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid IP address format: " + ipAddress));
            return true;
        }

        punishmentService.ipban(ipAddress, sender.getName(), reason)
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully IP banned &e" + ipAddress + " &afor: &f" + reason));
                    } else {
                        sender.sendMessage(MessageUtils.colorize("&cFailed to IP ban " + ipAddress + ". Please check the IP address."));
                    }
                })
                .exceptionally(throwable -> {
                    sender.sendMessage(MessageUtils.colorize("&cError executing IP ban: " + throwable.getMessage()));
                    return null;
                });

        return true;
    }

    private boolean isValidIP(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return false;
            
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            completions.addAll(Arrays.asList("192.168.", "10.0.", "127.0.0.1"));
        } else if (args.length == 2) {
            completions.addAll(Arrays.asList("Hacking", "Griefing", "Inappropriate behavior", "Spam"));
        }

        return completions;
    }
}
