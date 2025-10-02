package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import me.wethink.weGuardian.utils.TimeUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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

public class MutelistCommand implements CommandExecutor {

    private static final int ITEMS_PER_PAGE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeGuardian plugin;

    public MutelistCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.mutelist")) {
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
            List<Punishment> mutes = punishments.stream()
                    .filter(p -> p.getType() == PunishmentType.MUTE ||
                            p.getType() == PunishmentType.TEMPMUTE ||
                            (finalIncludeIP && (p.getType() == PunishmentType.IPMUTE)))
                    .filter(p -> finalFilter == null ||
                            p.getTargetName().toLowerCase().contains(finalFilter.toLowerCase()) ||
                            p.getReason().toLowerCase().contains(finalFilter.toLowerCase()))
                    .collect(Collectors.toList());

            if (mutes.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&aNo active mutes found" +
                        (finalFilter != null ? " matching filter '" + finalFilter + "'" : "") + "."));
                return;
            }

            int totalPages = (int) Math.ceil((double) mutes.size() / ITEMS_PER_PAGE);
            int startIndex = (finalPage - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, mutes.size());

            if (finalPage > totalPages) {
                sender.sendMessage(MessageUtils.colorize("&cPage " + finalPage + " does not exist. Maximum page: " + totalPages));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&6&l=== Mute List (Page " + finalPage + "/" + totalPages + ") ==="));
            if (finalFilter != null) {
                sender.sendMessage(MessageUtils.colorize("&7Filter: &f" + finalFilter));
            }
            if (finalIncludeIP) {
                sender.sendMessage(MessageUtils.colorize("&7Including IP mutes"));
            }
            sender.sendMessage(MessageUtils.colorize("&7Total mutes: &f" + mutes.size()));
            sender.sendMessage("");

            for (int i = startIndex; i < endIndex; i++) {
                Punishment mute = mutes.get(i);
                String target = mute.isIPBased() ? mute.getTargetIP() : mute.getTargetName();
                String type = mute.getType() == PunishmentType.IPMUTE ? "IP" : "Player";
                String muteType = mute.getType() == PunishmentType.TEMPMUTE ? "Temp" : "Perm";

                Component muteInfo = Component.text()
                        .append(Component.text((i + 1) + ". ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(target).color(MessageUtils.parseColor("&6")))
                        .append(Component.text(" (" + type + " - " + muteType + ")").color(MessageUtils.parseColor("&8")))
                        .build();

                Component hoverText = Component.text()
                        .append(Component.text("Target: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(target).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nStaff: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(mute.getStaffName()).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nReason: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(mute.getReason()).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nDate: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(mute.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                        .build();

                if (mute.getExpiresAt() != null) {
                    long timeLeft = Duration.between(LocalDateTime.now(), mute.getExpiresAt()).toMillis();
                    hoverText = Component.text()
                            .append(Component.text("Target: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(target).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nStaff: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getStaffName()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nReason: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getReason()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nDate: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nExpires: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getExpiresAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nTime Left: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(TimeUtils.formatDuration(timeLeft)).color(MessageUtils.parseColor("&f")))
                            .build();
                } else {
                    hoverText = Component.text()
                            .append(Component.text("Target: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(target).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nStaff: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getStaffName()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nReason: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getReason()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nDate: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(mute.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nType: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text("Permanent").color(MessageUtils.parseColor("&c")))
                            .build();
                }

                muteInfo = muteInfo.hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.suggestCommand("/unmute " + target));

                sender.sendMessage(muteInfo);

                sender.sendMessage(MessageUtils.colorize("   &7Reason: &f" + mute.getReason()));
                sender.sendMessage(MessageUtils.colorize("   &7Staff: &f" + mute.getStaffName() +
                        " &8| &7Date: &f" + mute.getCreatedAt().format(DATE_FORMAT)));

                if (mute.getExpiresAt() != null) {
                    long timeLeft = Duration.between(LocalDateTime.now(), mute.getExpiresAt()).toMillis();
                    sender.sendMessage(MessageUtils.colorize("   &7Expires: &f" + TimeUtils.formatDuration(timeLeft)));
                }
                sender.sendMessage("");
            }

            Component footer = Component.text("");
            if (finalPage > 1) {
                footer = Component.text("[◀ Previous]")
                        .color(MessageUtils.parseColor("&a"))
                        .clickEvent(ClickEvent.runCommand("/mutelist " + (finalPage - 1) +
                                (finalIncludeIP ? " --ip" : "") +
                                (finalFilter != null ? " --filter " + finalFilter : "")))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage - 1))));
            }

            footer = Component.text(" Page " + finalPage + "/" + totalPages + " ")
                    .color(MessageUtils.parseColor("&7"));

            if (finalPage < totalPages) {
                footer = Component.text("[Next ▶]")
                        .color(MessageUtils.parseColor("&a"))
                        .clickEvent(ClickEvent.runCommand("/mutelist " + (finalPage + 1) +
                                (finalIncludeIP ? " --ip" : "") +
                                (finalFilter != null ? " --filter " + finalFilter : "")))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage + 1))));
            }

            sender.sendMessage(footer);
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError retrieving mute list: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in mutelist command: " + throwable.getMessage());
            return null;
        });

        return true;
    }
}
