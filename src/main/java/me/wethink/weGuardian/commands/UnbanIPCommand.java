package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UnbanIPCommand implements CommandExecutor {

    private final PunishmentService punishmentService;
    private final WeGuardian plugin;

    public UnbanIPCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.unbanip")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /unbanip <ip|player>"));
            return true;
        }

        String target = args[0];
        String ipAddress;

        if (isValidIP(target)) {
            ipAddress = target;
        } else {
            CompletableFuture<PlayerData> playerDataFuture = plugin.getDatabaseManager().getPlayerData(target);
            PlayerData playerData = playerDataFuture.join();
            if (playerData != null && playerData.getLastIP() != null) {
                ipAddress = playerData.getLastIP();
                sender.sendMessage(MessageUtils.colorize("&aFound IP &e" + ipAddress + " &afor player &e" + target + "."));
            } else {
                sender.sendMessage(MessageUtils.colorize("&cCould not find IP address for player &e" + target + "."));
                return true;
            }
        }

        punishmentService.unbanIP(ipAddress, sender.getName())
                .thenAccept(success -> {
                    if (success) {
                        sender.sendMessage(MessageUtils.colorize("&aSuccessfully unbanned IP &e" + ipAddress));
                    } else {
                        sender.sendMessage(MessageUtils.colorize("&c" + ipAddress + " is not currently banned."));
                    }
                })
                .exceptionally(throwable -> {
                    sender.sendMessage(MessageUtils.colorize("&cError executing IP unban: " + throwable.getMessage()));
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
}