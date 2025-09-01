package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NotificationService {

    private final WeGuardian plugin;
    private final Map<UUID, BukkitRunnable> actionBarTasks = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executorService;

    public NotificationService(WeGuardian plugin) {
        this.plugin = plugin;
        this.executorService = Executors.newScheduledThreadPool(2);
    }

    private String getMessageTemplate(String context, PunishmentType type) {
        String messageKey = switch (context) {
            case "chat" -> "chat." + type.name().toLowerCase();
            case "reconnect_attempt" -> "chat.reconnect_attempt";
            case "chat_attempt" -> "chat.chat_attempt";
            default -> "defaults.generic_punishment";
        };

        return plugin.getMessage(messageKey);
    }

    private String getUnpunishmentTemplate(PunishmentType type) {
        String messageKey = switch (type) {
            case BAN, TEMPBAN -> "chat.unban";
            case MUTE, TEMPMUTE -> "chat.unmute";
            default -> "defaults.generic_unban";
        };

        return plugin.getMessage(messageKey);
    }

    public void broadcastPunishment(Punishment punishment) {
        broadcastPunishment(punishment, "chat");
    }

    public void broadcastPunishment(Punishment punishment, String context) {
        if (!plugin.getConfig().getBoolean("notifications.chat.enabled", true)) {
            return;
        }

        String template = getMessageTemplate(context, punishment.getType());
        String message = MessageUtils.formatPunishmentMessage(template, punishment);

        String permission = plugin.getConfig().getString("notifications.chat.broadcast_permission", "weguardian.notify");

        broadcastToStaff(message, permission);
        logToConsole(message);
    }

    public void broadcastUnpunishment(Punishment punishment, String staffName) {
        if (!plugin.getConfig().getBoolean("notifications.chat.enabled", true)) {
            return;
        }

        String template = getUnpunishmentTemplate(punishment.getType());
        String message = template.replace("{player}", punishment.getTargetName())
                .replace("{staff}", staffName);
        message = MessageUtils.colorize(message);

        String permission = plugin.getConfig().getString("notifications.chat.broadcast_permission", "weguardian.notify");

        broadcastToStaff(message, permission);
        logToConsole(message);
    }

    public void broadcastUnpunishment(Punishment punishment, String staffName, String reason) {
        broadcastUnpunishment(punishment, staffName);
    }

    public void sendActionBarMessage(Player player, Punishment punishment) {
        String template = getMessageTemplate("actionbar", punishment.getType());
        String message = MessageUtils.formatPunishmentMessage(template, punishment);

        if (!message.isEmpty()) {
            Component component = MessageUtils.toComponent(message);
            player.sendActionBar(component);
        }
    }

    @SuppressWarnings("unused")
    private void startActionBarUpdates(Punishment punishment) {
        if (!plugin.getConfig().getBoolean("notifications.actionbar_updates", true)) {
            return;
        }

        if (punishment.getType() != PunishmentType.TEMPBAN &&
                punishment.getType() != PunishmentType.TEMPMUTE) {
            return;
        }

        UUID targetUuid = punishment.getTargetUuid();
        stopActionBarUpdates(targetUuid);

        int interval = plugin.getConfig().getInt("notifications.actionbar_interval", 30) * 20;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                Player player = Bukkit.getPlayer(targetUuid);
                if (player == null) {
                    cancel();
                    return;
                }

                if (punishment.isExpired()) {
                    cancel();
                    actionBarTasks.remove(targetUuid);
                    return;
                }

                sendActionBarMessage(player, punishment);
            }
        };

        task.runTaskTimer(plugin, 0, interval);
        actionBarTasks.put(targetUuid, task);
    }

    private void stopActionBarUpdates(UUID playerUuid) {
        BukkitRunnable task = actionBarTasks.remove(playerUuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void broadcastToStaff(String message, String permission) {
        Component component = MessageUtils.toComponent(message);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
            }
        }
    }

    private void logToConsole(String message) {
        plugin.getLogger().info(message);
    }

    public void shutdown() {
        for (BukkitRunnable task : actionBarTasks.values()) {
            task.cancel();
        }
        actionBarTasks.clear();

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
