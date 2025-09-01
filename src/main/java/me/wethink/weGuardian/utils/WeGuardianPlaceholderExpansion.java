package me.wethink.weGuardian.utils;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class WeGuardianPlaceholderExpansion extends PlaceholderExpansion {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;

    public WeGuardianPlaceholderExpansion(WeGuardian plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "weguardian";
    }

    @Override
    public @NotNull String getAuthor() {
        return "WeThink";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) {
            return "";
        }

        if (params.equals("banned")) {
            CompletableFuture<Boolean> future = databaseManager.isPlayerBanned(player.getUniqueId());
            try {
                return future.get(1, TimeUnit.SECONDS) ? "true" : "false";
            } catch (Exception e) {
                return "false";
            }
        }

        if (params.equals("muted")) {
            CompletableFuture<Boolean> future = databaseManager.isPlayerMuted(player.getUniqueId());
            try {
                return future.get(1, TimeUnit.SECONDS) ? "true" : "false";
            } catch (Exception e) {
                return "false";
            }
        }

        if (params.equals("ban_reason")) {
            return getActivePunishmentInfo(player, PunishmentType.BAN, "reason");
        }

        if (params.equals("ban_staff")) {
            return getActivePunishmentInfo(player, PunishmentType.BAN, "staff");
        }

        if (params.equals("ban_expires")) {
            return getActivePunishmentInfo(player, PunishmentType.BAN, "expires");
        }

        if (params.equals("ban_time_left")) {
            return getActivePunishmentInfo(player, PunishmentType.BAN, "timeleft");
        }

        if (params.equals("tempban_reason")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPBAN, "reason");
        }

        if (params.equals("tempban_staff")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPBAN, "staff");
        }

        if (params.equals("tempban_expires")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPBAN, "expires");
        }

        if (params.equals("tempban_time_left")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPBAN, "timeleft");
        }

        if (params.equals("mute_reason")) {
            return getActivePunishmentInfo(player, PunishmentType.MUTE, "reason");
        }

        if (params.equals("mute_staff")) {
            return getActivePunishmentInfo(player, PunishmentType.MUTE, "staff");
        }

        if (params.equals("mute_expires")) {
            return getActivePunishmentInfo(player, PunishmentType.MUTE, "expires");
        }

        if (params.equals("mute_time_left")) {
            return getActivePunishmentInfo(player, PunishmentType.MUTE, "timeleft");
        }

        if (params.equals("tempmute_reason")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPMUTE, "reason");
        }

        if (params.equals("tempmute_staff")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPMUTE, "staff");
        }

        if (params.equals("tempmute_expires")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPMUTE, "expires");
        }

        if (params.equals("tempmute_time_left")) {
            return getActivePunishmentInfo(player, PunishmentType.TEMPMUTE, "timeleft");
        }

        if (params.equals("total_punishments")) {
            return getPunishmentCount(player, null);
        }

        if (params.equals("total_bans")) {
            return getPunishmentCount(player, PunishmentType.BAN);
        }

        if (params.equals("total_tempbans")) {
            return getPunishmentCount(player, PunishmentType.TEMPBAN);
        }

        if (params.equals("total_mutes")) {
            return getPunishmentCount(player, PunishmentType.MUTE);
        }

        if (params.equals("total_tempmutes")) {
            return getPunishmentCount(player, PunishmentType.TEMPMUTE);
        }

        if (params.equals("total_kicks")) {
            return getPunishmentCount(player, PunishmentType.KICK);
        }

        if (params.equals("total_warns")) {
            return getPunishmentCount(player, PunishmentType.WARN);
        }

        return null;
    }

    private String getActivePunishmentInfo(OfflinePlayer player, PunishmentType type, String info) {
        try {
            CompletableFuture<Punishment> future = databaseManager.getActivePunishment(player.getUniqueId(), type);
            Punishment punishment = future.get(1, TimeUnit.SECONDS);

            if (punishment == null) {
                return "";
            }

            switch (info.toLowerCase()) {
                case "reason":
                    return punishment.getReason();
                case "staff":
                    return punishment.getStaffName();
                case "expires":
                    if (punishment.getExpiresAt() == null) {
                        return "Never";
                    }
                    return formatTimestamp(punishment.getExpiresAt());
                case "timeleft":
                    if (punishment.getExpiresAt() == null) {
                        return "Permanent";
                    }
                    long timeLeft = punishment.getExpiresAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis();
                    if (timeLeft <= 0) {
                        return "Expired";
                    }
                    return formatDuration(timeLeft);
                default:
                    return "";
            }
        } catch (Exception e) {
            return "";
        }
    }

    private String getPunishmentCount(OfflinePlayer player, PunishmentType type) {
        try {
            CompletableFuture<List<Punishment>> future;
            if (type == null) {
                future = databaseManager.getPunishmentHistory(player.getUniqueId());
            } else {
                future = databaseManager.getPunishmentHistory(player.getUniqueId(), type);
            }

            List<Punishment> punishments = future.get(1, TimeUnit.SECONDS);
            return String.valueOf(punishments.size());
        } catch (Exception e) {
            return "0";
        }
    }

    private String formatTimestamp(LocalDateTime dateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
}
