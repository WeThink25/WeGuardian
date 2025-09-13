package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import com.tcoded.folialib.wrapper.task.WrappedTask;

public class NotificationService {

    private final WeGuardian plugin;
    private final Map<UUID, WrappedTask> actionBarTasks = new ConcurrentHashMap<>();
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
        plugin.debug("Broadcasting punishment notification: type=%s, target=%s, staff=%s", 
                punishment.getType(), punishment.getTargetName(), punishment.getStaffName());
        broadcastPunishment(punishment, "chat");
    }

    public void broadcastPunishment(Punishment punishment, String context) {
        if (!plugin.getConfig().getBoolean("notifications.chat.enabled", true)) {
            plugin.debug("Punishment notification skipped: chat notifications disabled");
            return;
        }

        String template = getMessageTemplate(context, punishment.getType());
        String message = MessageUtils.formatPunishmentMessage(template, punishment);
        plugin.debug("Formatted punishment message: %s", message);

        String permission = plugin.getConfig().getString("notifications.chat.broadcast_permission", "weguardian.notify");
        plugin.debug("Broadcasting to staff with permission: %s", permission);

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

        var task = plugin.getFoliaLib().getScheduler().runTimer(() -> {
            Player player = Bukkit.getPlayer(targetUuid);
            if (player == null) {
                stopActionBarUpdates(targetUuid);
                return;
            }

            if (punishment.isExpired()) {
                stopActionBarUpdates(targetUuid);
                return;
            }

            sendActionBarMessage(player, punishment);
        }, 0, interval);
        
        actionBarTasks.put(targetUuid, task);
    }

    private void stopActionBarUpdates(UUID playerUuid) {
        WrappedTask task = actionBarTasks.remove(playerUuid);
        if (task != null) {
            plugin.getFoliaLib().getScheduler().cancelTask(task);
        }
    }

    private void broadcastToStaff(String message, String permission) {
        Component component = MessageUtils.toComponent(message);
        int notifiedCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission(permission)) {
                player.sendMessage(component);
                notifiedCount++;
            }
        }
        plugin.debug("Notified %d staff members with permission: %s", notifiedCount, permission);
    }

    private void logToConsole(String message) {
        plugin.getLogger().info(message);
    }

    public void shutdown() {
        for (WrappedTask task : actionBarTasks.values()) {
            plugin.getFoliaLib().getScheduler().cancelTask(task);
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
