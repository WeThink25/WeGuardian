package me.wethink.weguardian.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.MessagesManager;
import me.wethink.weguardian.util.TimeUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class CustomDurationHandler implements Listener {

    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;
    private volatile boolean completed = false;

    public CustomDurationHandler(WeGuardian plugin, Player staff, OfflinePlayer target, PunishmentType type) {
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
    }

    public void start() {
        MessagesManager msg = plugin.getMessagesManager();

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.header")));
        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.title")));
        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.format-header")));

        List<String> formats = msg.getMessageList("input.custom-duration.formats");
        for (String format : formats) {
            staff.sendMessage(MessageUtil.toComponent(format));
        }

        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.instruction")));
        staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.header")));

        plugin.getSchedulerManager().runAsyncLater(this::handleTimeout, 60, TimeUnit.SECONDS);
    }

    private void handleTimeout() {
        if (!completed) {
            cleanup();
            plugin.getSchedulerManager().runForEntity(staff,
                    () -> staff.sendMessage(MessageUtil
                            .toComponent(plugin.getMessagesManager().getMessage("input.custom-duration.timeout"))));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(staff.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = PLAIN_SERIALIZER.serialize(event.message());
        MessagesManager msg = plugin.getMessagesManager();

        if (message.equalsIgnoreCase("cancel")) {
            cleanup();
            staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.cancelled")));
            PunishmentGUI.openAsync(plugin, staff, target);
            return;
        }

        long durationMs = TimeUtil.parseDuration(message);
        if (durationMs <= 0) {
            staff.sendMessage(MessageUtil.toComponent(msg.getMessage("input.custom-duration.invalid")));
            return;
        }

        completed = true;
        cleanup();

        staff.sendMessage(MessageUtil.toComponent(
                msg.getMessage("input.custom-duration.success", "{duration}", TimeUtil.formatDuration(durationMs))));

        new ReasonInputHandler(plugin, staff, target, type, durationMs).start();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(staff.getUniqueId())) {
            cleanup();
        }
    }

    private void cleanup() {
        completed = true;
        HandlerList.unregisterAll(this);
    }
}
