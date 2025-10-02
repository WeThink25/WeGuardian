package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class WarnsCommand implements CommandExecutor {

    private static final int ITEMS_PER_PAGE = 10;
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeGuardian plugin;

    public WarnsCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.warns")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /warns <player> [page]"));
            return true;
        }

        String targetName = args[0];
        int page = 1;

        if (args.length > 1) {
            try {
                page = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                sender.sendMessage(MessageUtils.colorize("&cInvalid page number: " + args[1]));
                return true;
            }
        }

        final int finalPage = page;

        CompletableFuture<java.util.UUID> uuidFuture = plugin.getPunishmentService().getPlayerUUID(targetName);

        uuidFuture.thenCompose(uuid -> {
            if (uuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer '" + targetName + "' not found."));
                return CompletableFuture.completedFuture(null);
            }

            return plugin.getDatabaseManager().getPunishmentHistory(uuid);
        }).thenAccept(punishments -> {
            if (punishments == null) return;

            List<Punishment> warnings = punishments.stream()
                    .filter(p -> p.getType() == PunishmentType.WARN)
                    .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) 
                    .collect(Collectors.toList());

            if (warnings.isEmpty()) {
                sender.sendMessage(MessageUtils.colorize("&aNo warnings found for player '" + targetName + "'."));
                return;
            }

            int totalPages = (int) Math.ceil((double) warnings.size() / ITEMS_PER_PAGE);
            int startIndex = (finalPage - 1) * ITEMS_PER_PAGE;
            int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, warnings.size());

            if (finalPage > totalPages) {
                sender.sendMessage(MessageUtils.colorize("&cPage " + finalPage + " does not exist. Maximum page: " + totalPages));
                return;
            }

            sender.sendMessage(MessageUtils.colorize("&6&l=== Warnings for " + targetName + " (Page " + finalPage + "/" + totalPages + ") ==="));
            sender.sendMessage(MessageUtils.colorize("&7Total warnings: &f" + warnings.size()));
            sender.sendMessage("");

            for (int i = startIndex; i < endIndex; i++) {
                Punishment warning = warnings.get(i);

                Component warningInfo = Component.text()
                        .append(Component.text((i + 1) + ". ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text("ID: " + warning.getId()).color(MessageUtils.parseColor("&e")))
                        .append(Component.text(" | ").color(MessageUtils.parseColor("&8")))
                        .append(Component.text(warning.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&7")))
                        .build();

                Component hoverText = Component.text()
                        .append(Component.text("Warning ID: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(warning.getId() + "").color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nTarget: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(targetName).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nStaff: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(warning.getStaffName()).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nReason: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(warning.getReason()).color(MessageUtils.parseColor("&f")))
                        .append(Component.text("\nDate: ").color(MessageUtils.parseColor("&7")))
                        .append(Component.text(warning.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                        .build();

                if (!warning.isActive() && warning.getRemovedBy() != null) {
                    hoverText = Component.text()
                            .append(Component.text("Warning ID: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(warning.getId() + "").color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nTarget: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(targetName).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nStaff: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(warning.getStaffName()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nReason: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(warning.getReason()).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nDate: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(warning.getCreatedAt().format(DATE_FORMAT)).color(MessageUtils.parseColor("&f")))
                            .append(Component.text("\nRemoved By: ").color(MessageUtils.parseColor("&7")))
                            .append(Component.text(warning.getRemovedBy()).color(MessageUtils.parseColor("&f")))
                            .build();
                }

                warningInfo = warningInfo.hoverEvent(HoverEvent.showText(hoverText))
                        .clickEvent(ClickEvent.suggestCommand("/rollback " + warning.getId()));

                sender.sendMessage(warningInfo);

                sender.sendMessage(MessageUtils.colorize("   &7Reason: &f" + warning.getReason()));
                sender.sendMessage(MessageUtils.colorize("   &7Staff: &f" + warning.getStaffName()));

                if (warning.isActive()) {
                    sender.sendMessage(MessageUtils.colorize("   &7Status: &aActive"));
                } else {
                    String removedBy = warning.getRemovedBy() != null ? warning.getRemovedBy() : "System";
                    sender.sendMessage(MessageUtils.colorize("   &7Status: &cRemoved by " + removedBy));
                }

                sender.sendMessage("");
            }

            Component footer = Component.text("");
            if (finalPage > 1) {
                footer = Component.text("[◀ Previous]").color(MessageUtils.parseColor("&a"))
                        .clickEvent(ClickEvent.runCommand("/warns " + targetName + " " + (finalPage - 1)))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage - 1))));
            }

            footer = Component.text(" Page " + finalPage + "/" + totalPages + " ").color(MessageUtils.parseColor("&7"));

            if (finalPage < totalPages) {
                footer = Component.text("[Next ▶]").color(MessageUtils.parseColor("&a"))
                        .clickEvent(ClickEvent.runCommand("/warns " + targetName + " " + (finalPage + 1)))
                        .hoverEvent(HoverEvent.showText(Component.text("Go to page " + (finalPage + 1))));
            }

            sender.sendMessage(footer);
            sender.sendMessage(MessageUtils.colorize("&7Click on a warning ID to view blame information."));

        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError retrieving warnings: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in warns command: " + throwable.getMessage());
            return null;
        });

        return true;
    }


}
