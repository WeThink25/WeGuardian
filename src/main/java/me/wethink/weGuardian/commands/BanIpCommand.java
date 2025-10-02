package me.wethink.weGuardian.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandCompletion;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Description;
import co.aikar.commands.annotation.Syntax;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.services.PunishmentService;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@CommandAlias("banip")
@Description("IP ban a player or IP address")
public class BanIpCommand extends BaseCommand {

    private final PunishmentService punishmentService;
    private final WeGuardian plugin;

    public BanIpCommand(WeGuardian plugin, PunishmentService punishmentService) {
        this.plugin = plugin;
        this.punishmentService = punishmentService;
    }

    @CommandCompletion("@players @reasons|@templates -s -t <template>")
    @Syntax("<ip|player> <reason> [-s] [-t template]")
    @CommandPermission("weguardian.banip")
    public void onBanIp(CommandSender sender, String target, String[] reasonArgs) {
        String reason = String.join(" ", reasonArgs);

        if (reason.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /banip <ip|player> <reason>"));
            return;
        }

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
                return;
            }
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
