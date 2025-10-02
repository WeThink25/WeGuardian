package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.services.TemplateService;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


public class TempIPBanCommand implements CommandExecutor {

    private final WeGuardian plugin;
    private final PunishmentService punishmentService;
    private final TemplateService templateService;

    public TempIPBanCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
        this.templateService = plugin.getTemplateService();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.tempipban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /tempipban <ip|player> <duration> <reason> [-s] [-t template]"));
            sender.sendMessage(MessageUtils.colorize("&7Examples:"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempipban 192.168.1.1 1d Griefing"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempipban 10.0.0.1 2h Spam -s"));
            sender.sendMessage(MessageUtils.colorize("&7  /tempipban 127.0.0.1 1w Hacking -t cheating"));
            sender.sendMessage(MessageUtils.colorize("&7Options:"));
            sender.sendMessage(MessageUtils.colorize("&7  -s: Silent tempipban (no broadcast)"));
            sender.sendMessage(MessageUtils.colorize("&7  -t <template>: Use punishment template"));
            return true;
        }

        String target = args[0];
        String duration = args[1];
        boolean silent = Arrays.asList(args).contains("-s");
        String template = null;

        for (int i = 0; i < args.length - 1; i++) {
            if (args[i].equals("-t") && i + 1 < args.length) {
                template = args[i + 1];
                break;
            }
        }
        final String finalTemplate = template;

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

        if (!TimeUtils.isValidTimeFormat(duration)) {
            sender.sendMessage(MessageUtils.colorize("&cInvalid time format. Use formats like: 1h, 2d, 1w, 1m, 1y"));
            return true;
        }

        List<String> reasonParts = new ArrayList<>();
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];
            if (arg.equals("-s")) continue;
            if (arg.equals("-t")) {
                i++;
                continue;
            }
            reasonParts.add(arg);
        }

        if (reasonParts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cYou must provide a reason for the tempipban."));
            return true;
        }

        String reason = String.join(" ", reasonParts);
        String staffName = sender.getName();

        if (finalTemplate != null && templateService.hasTemplate(finalTemplate)) {
            templateService.executePunishmentFromTemplate(null, ipAddress, staffName, finalTemplate, duration)
                    .thenAccept(success -> {
                        if (success) {
                            sender.sendMessage(MessageUtils.colorize("&aSuccessfully applied template '" + finalTemplate +
                                    "' punishment to IP " + ipAddress + " for " + duration));
                        } else {
                            sender.sendMessage(MessageUtils.colorize("&cFailed to apply template punishment."));
                        }
                    });
        } else {
            punishmentService.ipTempban(ipAddress, staffName, reason, duration)
                    .thenAccept(success -> {
                        if (success) {
                            String message = "&aSuccessfully IP tempbanned &e" + ipAddress + " &afor " + duration + ": &f" + reason;
                            if (silent) {
                                message += " &7(Silent)";
                            }
                            sender.sendMessage(MessageUtils.colorize(message));
                            plugin.getLogger().info(staffName + " IP tempbanned " + ipAddress + " for " + duration +
                                    ": " + reason + (silent ? " (Silent)" : ""));
                        } else {
                            sender.sendMessage(MessageUtils.colorize("&cFailed to IP tempban " + ipAddress + ". Please check the IP address."));
                        }
                    })
                    .exceptionally(throwable -> {
                        sender.sendMessage(MessageUtils.colorize("&cError executing IP tempban: " + throwable.getMessage()));
                        return null;
                    });
        }

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