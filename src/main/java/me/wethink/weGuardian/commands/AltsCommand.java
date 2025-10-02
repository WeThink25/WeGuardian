package me.wethink.weGuardian.commands;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class AltsCommand implements CommandExecutor {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private final WeGuardian plugin;

    public AltsCommand(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (!sender.hasPermission("weguardian.alts")) {
            sender.sendMessage(MessageUtils.colorize("&cYou don't have permission to use this command."));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(MessageUtils.colorize("&cUsage: /alts <player> [--strict] [--punished] [--truealts]"));
            return true;
        }

        String targetName = args[0];
        boolean strict = Arrays.asList(args).contains("--strict");
        boolean punishedOnly = Arrays.asList(args).contains("--punished");
        boolean requireSharedPunishments = Arrays.asList(args).contains("--truealts");

        CompletableFuture<UUID> uuidFuture = plugin.getPunishmentService().getPlayerUUID(targetName);

        uuidFuture.thenCompose(uuid -> {
            if (uuid == null) {
                sender.sendMessage(MessageUtils.colorize("&cPlayer \'" + targetName + "\' not found."));
                return CompletableFuture.completedFuture(null);
            }

            return findAltAccounts(uuid, targetName, strict, punishedOnly, requireSharedPunishments);
        }).thenAccept(altData -> {
            if (altData == null) return;

            displayAltAccounts(sender, targetName, altData, strict, punishedOnly, requireSharedPunishments);
        }).exceptionally(throwable -> {
            sender.sendMessage(MessageUtils.colorize("&cError checking alt accounts: " + throwable.getMessage()));
            plugin.getLogger().severe("Error in alts command: " + throwable.getMessage());
            return null;
        });

        return true;
    }

    private CompletableFuture<AltAccountData> findAltAccounts(UUID targetUuid, String targetName, boolean strict, boolean punishedOnly, boolean requireSharedPunishments) {
        long expirationTimeMillis = System.currentTimeMillis() - (60L * 24 * 60 * 60 * 1000); // 60 days in milliseconds
        return plugin.getDatabaseManager().getPlayerConnections(targetUuid).thenCompose(connections -> {
            if (connections.isEmpty()) {
                return CompletableFuture.completedFuture(new AltAccountData(new ArrayList<>()));
            }

            Set<String> targetIPs = connections.stream()
                    .map(DatabaseManager.PlayerConnection::getIp)
                    .collect(Collectors.toSet());

            List<CompletableFuture<List<DatabaseManager.PlayerConnection>>> ipFutures = targetIPs.stream()
                    .map(ip -> plugin.getDatabaseManager().getPlayersFromIP(ip, expirationTimeMillis))
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(ipFutures.toArray(new CompletableFuture[0]))
                    .thenCompose(v -> {
                        Set<UUID> altUuids = new HashSet<>();
                        Map<UUID, Set<String>> playerIPMap = new HashMap<>();

                        for (CompletableFuture<List<DatabaseManager.PlayerConnection>> future : ipFutures) {
                            List<DatabaseManager.PlayerConnection> ipConnections = future.join();
                            for (DatabaseManager.PlayerConnection conn : ipConnections) {
                                UUID uuid = conn.getUuid();
                                if (!uuid.equals(targetUuid)) {
                                    altUuids.add(uuid);
                                    playerIPMap.computeIfAbsent(uuid, k -> new HashSet<>()).add(conn.getIp());
                                }
                            }
                        }

                        Set<UUID> filteredAltUuids;
                        if (strict) {
                            filteredAltUuids = altUuids.stream()
                                    .filter(uuid -> {
                                        Set<String> sharedIPs = new HashSet<>(targetIPs);
                                        sharedIPs.retainAll(playerIPMap.get(uuid));
                                        return sharedIPs.size() > 1;
                                    })
                                    .collect(Collectors.toSet());
                        } else {
                            filteredAltUuids = altUuids.stream()
                                    .filter(uuid -> {
                                        Set<String> playerIPs = playerIPMap.get(uuid);
                                        return playerIPs.stream().anyMatch(targetIPs::contains);
                                    })
                                    .collect(Collectors.toSet());
                        }

                        List<CompletableFuture<List<Punishment>>> punishmentFutures = filteredAltUuids.stream()
                                .map(uuid -> plugin.getDatabaseManager().getPunishmentHistory(uuid))
                                .collect(Collectors.toList());

                        return CompletableFuture.allOf(punishmentFutures.toArray(new CompletableFuture[0]))
                                .thenApply(vv -> {
                                    List<AltAccount> alts = new ArrayList<>();
                                    
                                    int i = 0;
                                    for (UUID altUuid : filteredAltUuids) {
                                        List<Punishment> punishments = punishmentFutures.get(i++).join();
                                        
                                        if (requireSharedPunishments && punishments.stream().noneMatch(p -> p.isActive() &&
                                                (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN))) {
                                            continue;
                                        }

                                        if (punishedOnly && punishments.stream().noneMatch(p -> p.isActive() &&
                                                (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN))) {
                                            continue;
                                        }

                                        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(altUuid);
                                        String altName = offlinePlayer.getName() != null ? offlinePlayer.getName() : "Unknown";

                                        Set<String> sharedIPs = new HashSet<>(targetIPs);
                                        sharedIPs.retainAll(playerIPMap.get(altUuid));

                                        boolean isOnline = offlinePlayer.isOnline();
                                        long lastSeen = offlinePlayer.getLastSeen();

                                        alts.add(new AltAccount(altUuid, altName, sharedIPs, punishments, isOnline, lastSeen));
                                    }

                                    alts.sort((a, b) -> {
                                        boolean aBanned = a.punishments.stream().anyMatch(p -> p.isActive() &&
                                                (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN));
                                        boolean bBanned = b.punishments.stream().anyMatch(p -> p.isActive() &&
                                                (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN));

                                        if (aBanned != bBanned) return aBanned ? -1 : 1;
                                        return Long.compare(b.lastSeen, a.lastSeen);
                                    });

                                    return new AltAccountData(alts);
                                });
                    });
        });
    }

    private void displayAltAccounts(CommandSender sender, String targetName, AltAccountData data, boolean strict, boolean punishedOnly, boolean requireSharedPunishments) {
        sender.sendMessage(MessageUtils.colorize("&6&l=== Alt Accounts for " + targetName + " ==="));

        if (strict) {
            sender.sendMessage(MessageUtils.colorize("&7Mode: &eStrict (multiple shared IPs)"));
        }
        if (punishedOnly) {
            sender.sendMessage(MessageUtils.colorize("&7Filter: &cPunished accounts only"));
        }
        if (requireSharedPunishments) {
            sender.sendMessage(MessageUtils.colorize("&7Filter: &bTrue Alts (shared active bans/tempbans)"));
            data.alts.removeIf(alt -> alt.punishments.stream().noneMatch(p -> p.isActive() &&
                    (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN)));
        }

        sender.sendMessage(MessageUtils.colorize("&7Found: &f" + data.alts.size() + " potential alt account(s)"));
        sender.sendMessage("");

        if (data.alts.isEmpty()) {
            sender.sendMessage(MessageUtils.colorize("&aNo alt accounts found."));
            return;
        }

        for (int i = 0; i < data.alts.size(); i++) {
            AltAccount alt = data.alts.get(i);

            TextColor statusColor = TextColor.fromHexString("FFFFFF");
            String statusText = "Clean";

            boolean isBanned = alt.punishments.stream().anyMatch(p -> p.isActive() &&
                    (p.getType() == PunishmentType.BAN || p.getType() == PunishmentType.TEMPBAN));
            boolean isMuted = alt.punishments.stream().anyMatch(p -> p.isActive() &&
                    (p.getType() == PunishmentType.MUTE || p.getType() == PunishmentType.TEMPMUTE));

            if (isBanned) {
                statusColor = TextColor.fromHexString("FF0000");
                statusText = "BANNED";
            } else if (isMuted) {
                statusColor = TextColor.fromHexString("FFA07A");
                statusText = "MUTED";
            } else if (!alt.punishments.isEmpty()) {
                statusColor = TextColor.fromHexString("FFD700");
                statusText = "History";
            }

            String onlineStatus = alt.isOnline ? "&a[ONLINE]" : "&7[OFFLINE]";

            Component altInfo = Component.text()
                    .append(Component.text((i + 1) + ". ").color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(alt.name).color(TextColor.fromHexString("FFFFFF")))
                    .append(Component.text(" " + onlineStatus).color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(" [" + statusText + "]").color(statusColor))
                    .build();

            Component hoverText = Component.text()
                    .append(Component.text("Player: ").color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(alt.name).color(TextColor.fromHexString("FFFFFF")))
                    .append(Component.text("\nUUID: ").color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(alt.uuid.toString()).color(TextColor.fromHexString("FFFFFF")))
                    .append(Component.text("\nShared IPs: ").color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(alt.sharedIPs.size() + "").color(TextColor.fromHexString("FFFFFF")))
                    .append(Component.text("\nPunishments: ").color(TextColor.fromHexString("AAAAAA")))
                    .append(Component.text(alt.punishments.size() + "").color(TextColor.fromHexString("FFFFFF")))
                    .build();

            if (!alt.isOnline && alt.lastSeen > 0) {
                Component lastSeenComponent = Component.text()
                        .append(Component.text("\nLast Seen: ").color(TextColor.fromHexString("AAAAAA")))
                        .append(Component.text(new Date(alt.lastSeen).toString()).color(TextColor.fromHexString("FFFFFF")))
                        .build();

                hoverText = Component.text()
                        .append(hoverText)
                        .append(lastSeenComponent)
                        .build();
            }

            altInfo = altInfo.hoverEvent(HoverEvent.showText(hoverText))
                    .clickEvent(ClickEvent.runCommand("/history " + alt.name));

            sender.sendMessage(altInfo);

            sender.sendMessage(MessageUtils.colorize("&7Shared IPs: &f" +
                    alt.sharedIPs.stream().limit(3).collect(Collectors.joining(", ")) +
                    (alt.sharedIPs.size() > 3 ? " &7(+" + (alt.sharedIPs.size() - 3) + " more)" : "")));

            List<Punishment> recentPunishments = alt.punishments.stream()
                    .filter(p -> p.isActive() || p.getCreatedAt().isAfter(java.time.LocalDateTime.now().minusDays(30)))
                    .limit(2)
                    .collect(Collectors.toList());

            if (!recentPunishments.isEmpty()) {
                for (Punishment punishment : recentPunishments) {
                    TextColor typeColor = punishment.isActive() ? TextColor.fromHexString("FF0000") : TextColor.fromHexString("AAAAAA");
                    sender.sendMessage(MessageUtils.colorize("   " + typeColor + punishment.getType().name() +
                            " &8| &7" + punishment.getReason() + " &8(" + punishment.getCreatedAt().format(DATE_FORMAT) + ")"));
                }
            }

            sender.sendMessage("");
        }


        sender.sendMessage(MessageUtils.colorize("&7Click on a player name to view their full punishment history."));
    }



    private static class AltAccountData {
        final List<AltAccount> alts;

        AltAccountData(List<AltAccount> alts) {
            this.alts = alts;
        }
    }

    private static class AltAccount {
        final UUID uuid;
        final String name;
        final Set<String> sharedIPs;
        final List<Punishment> punishments;
        final boolean isOnline;
        final long lastSeen;

        AltAccount(UUID uuid, String name, Set<String> sharedIPs, List<Punishment> punishments, boolean isOnline, long lastSeen) {
            this.uuid = uuid;
            this.name = name;
            this.sharedIPs = sharedIPs;
            this.punishments = punishments;
            this.isOnline = isOnline;
            this.lastSeen = lastSeen;
        }
    }
}
