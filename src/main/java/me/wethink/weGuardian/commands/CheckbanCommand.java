package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class CheckbanCommand implements CommandExecutor, TabCompleter {

    private final WeGuardian plugin;

    public CheckbanCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("weguardian.checkban")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length != 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /checkban <player>"));
            return true;
        }

        String targetName = args[0];

        plugin.getDatabaseManager().getPlayerData(targetName).thenAccept(playerData -> {
            if (playerData == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer not found in database."));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&6&l=== Punishment Status for " + targetName + " ==="));

            CompletableFuture<Punishment> banCheck = plugin.getDatabaseManager().getActivePunishment(playerData.getUuid(), PunishmentType.BAN)
                    .thenCompose(ban -> ban != null ? CompletableFuture.completedFuture(ban) :
                            plugin.getDatabaseManager().getActivePunishment(playerData.getUuid(), PunishmentType.TEMPBAN));

            CompletableFuture<Punishment> muteCheck = plugin.getDatabaseManager().getActivePunishment(playerData.getUuid(), PunishmentType.MUTE)
                    .thenCompose(mute -> mute != null ? CompletableFuture.completedFuture(mute) :
                            plugin.getDatabaseManager().getActivePunishment(playerData.getUuid(), PunishmentType.TEMPMUTE));

            CompletableFuture.allOf(banCheck, muteCheck).thenRun(() -> {
                try {
                    Punishment banPunishment = banCheck.get();
                    Punishment mutePunishment = muteCheck.get();

                    if (banPunishment == null && mutePunishment == null) {
                        sender.sendMessage(MessageUtils.colorize("&a✓ Player is not banned or muted"));
                        return;
                    }

                    if (banPunishment != null) {
                        displayPunishmentInfo(sender, banPunishment, "BAN");
                    }

                    if (mutePunishment != null) {
                        displayPunishmentInfo(sender, mutePunishment, "MUTE");
                    }

                } catch (Exception e) {
                    sender.sendMessage(MessageUtils.colorize("&cError checking punishment status."));
                }
            });
        });

        return true;
    }

    private void displayPunishmentInfo(CommandSender sender, Punishment punishment, String type) {
        sender.sendMessage(MessageUtils.colorize("&c✗ " + type + " STATUS: &4ACTIVE"));
        sender.sendMessage(MessageUtils.colorize("&7  Reason: &f" + punishment.getReason()));
        sender.sendMessage(MessageUtils.colorize("&7  Staff: &f" + punishment.getStaffName()));
        sender.sendMessage(MessageUtils.colorize("&7  Date: &f" + TimeUtils.formatDateTime(punishment.getCreatedAt())));

        if (punishment.getExpiresAt() != null) {
            if (punishment.getExpiresAt().isAfter(LocalDateTime.now())) {
                String timeLeft = TimeUtils.getRemainingTime(punishment.getExpiresAt());
                sender.sendMessage(MessageUtils.colorize("&7  Expires: &f" + TimeUtils.formatDateTime(punishment.getExpiresAt()) + " &8(" + timeLeft + " remaining)"));
            } else {
                sender.sendMessage(MessageUtils.colorize("&7  Status: &c&lEXPIRED &8(should be auto-removed)"));
            }
        } else {
            sender.sendMessage(MessageUtils.colorize("&7  Duration: &fPermanent"));
        }
        sender.sendMessage("");
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
