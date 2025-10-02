package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class BanlistCommand implements CommandExecutor {

    private static final int ITEMS_PER_PAGE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeGuardian plugin;

    public BanlistCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.banlist")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        int page = 1;
        String filter = null;
        boolean includeIP = false;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.equalsIgnoreCase("--ip")) {
                includeIP = true;
            } else if (arg.equalsIgnoreCase("--page") && i + 1 < args.length) {
                try {
                    page = Integer.parseInt(args[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.colorize("&cInvalid page number: " + args[i + 1]));
                    return true;
                }
            } else if (arg.equalsIgnoreCase("--filter") && i + 1 < args.length) {
                filter = args[i + 1];
                i++;
            } else if (arg.matches("\\d+")) {
                try {
                    page = Integer.parseInt(arg);
                } catch (NumberFormatException e) {
                    sender.sendMessage(MessageUtils.colorize("&cInvalid page number: " + arg));
                    return true;
                }
            }
        }

        final int finalPage = page;
        final String finalFilter = filter;
        final boolean finalIncludeIP = includeIP;

        plugin.getDatabaseManager().getAllActivePunishments().thenAccept(punishments -> {
            List<Punishment> bans = punishments.stream()
                    .filter(p -> p.getType() == PunishmentType.BAN ||
                            (finalIncludeIP && p.getType() == PunishmentType.IPBAN))
                    .filter(p -> finalFilter == null ||
                            p.getTargetName().toLowerCase().contains(finalFilter.toLowerCase()) ||
                            p.getReason().toLowerCase().contains(finalFilter.toLowerCase()))
                    .collect(Collectors.toList());

            if (bans.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&aNo active bans found" +
                        (finalFilter != null ? " matching filter '" + finalFilter + "'" : "") + "."));
                return;
            }

            int totalPages = (int) Math.ceil((double) bans.size() / ITEMS_PER_PAGE);
            int startIndex = (finalPage - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, bans.size());

            if (finalPage > totalPages) {
                sender.sendMessage(MessageUtils.colorize("&cPage " + finalPage + " does not exist. Maximum page: " + totalPages));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&6&l=== Ban List (Page " + finalPage + "/" + totalPages + ") ==="));
            if (finalFilter != null) {
                sender.sendMessage(MessageUtils.colorize("&7Filter: &f" + finalFilter));
            }
            if (finalIncludeIP) {
                sender.sendMessage(MessageUtils.colorize("&7Including IP bans"));
            }
            sender.sendMessage(MessageUtils.colorize("&7Total bans: &f" + bans.size()));
            sender.sendMessage("");

            for (int i = startIndex; i < endIndex; i++) {
                Punishment ban = bans.get(i);
                String target = ban.isIPBased() ? ban.getTargetIP() : ban.getTargetName();
                String type = ban.getType() == PunishmentType.IPBAN ? "IP" : "Player";
                String banType = ban.getExpiresAt() != null ? "Temporary" : "Permanent";

                Component banInfo = Component.text()
                        .append(Component.text((i + 1) + ". ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(target).color(TextColor.fromHexString("FFA07A")))
                        .append(Component.text(" (" + type + " - " + banType + ")").color(TextColor.fromHexString("808080")))
                        .build();

                Component hoverText = Component.text()
                        .append(Component.text("Target: ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(target).color(TextColor.fromHexString("FFFFFF")))
                        .append(Component.text("\nStaff: ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(ban.getStaffName()).color(TextColor.fromHexString("FFFFFF")))
                        .append(Component.text("\nReason: ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(ban.getReason()).color(TextColor.fromHexString("FFFFFF")))
                        .append(Component.text("\nDate: ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(ban.getCreatedAt().format(DATE_FORMAT)).color(TextColor.fromHexString("FFFFFF")))
                        .build();

                if (ban.getExpiresAt() != null) {
                    long timeLeft = Duration.between(LocalDateTime.now(), ban.getExpiresAt()).toMillis();
                    hoverText = Component.text()
                            .append(Component.text("Target: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(target).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nStaff: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getStaffName()).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nReason: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getReason()).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nDate: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getCreatedAt().format(DATE_FORMAT)).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nExpires: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getExpiresAt().format(DATE_FORMAT)).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nTime Left: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(TimeUtils.formatDuration(timeLeft)).color(TextColor.fromHexString("FFFFFF")))
                            .build();
                } else {
                    hoverText = Component.text()
                            .append(Component.text("Target: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(target).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nStaff: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getStaffName()).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nReason: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getReason()).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nDate: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text(ban.getCreatedAt().format(DATE_FORMAT)).color(TextColor.fromHexString("FFFFFF")))
                            .append(Component.text("\nType: ").color(TextColor.fromHexString("AAAAAA")))
                            .append(Component.text("Permanent").color(TextColor.fromHexString("FF0000")))
                            .build();
                }

                banInfo = banInfo.hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.suggestCommand("/blame " + ban.getId()));

                sender.sendMessage(banInfo);
            }

            Component footer = Component.text("");
            if (finalPage > 1) {
                footer = footer.append(Component.text("[◀ Previous]").color(TextColor.fromHexString("00FF00"))
                        .clickEvent(ClickEvent.runCommand("/banlist " + (finalPage - 1) +
                                (finalIncludeIP ? " --ip" : "") +
                                (finalFilter != null ? " --filter " + finalFilter : "")))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage - 1)))));
            }

            footer = footer.append(Component.text(" Page " + finalPage + "/" + totalPages + " ").color(TextColor.fromHexString("AAAAAA")));

            if (finalPage < totalPages) {
                footer = footer.append(Component.text("[Next ▶]").color(TextColor.fromHexString("00FF00"))
                        .clickEvent(ClickEvent.runCommand("/banlist " + (finalPage + 1) +
                                (finalIncludeIP ? " --ip" : "") +
                                (finalFilter != null ? " --filter " + finalFilter : "")))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage + 1)))));
            }

            sender.sendMessage(footer);
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError retrieving ban list: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in banlist command: " + throwable.getMessage());
            return null;
        });

        return true;
    }

}
