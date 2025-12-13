package me.wethink.weguardian.gui;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.PunishmentType;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.TimeUtil;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

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
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        staff.sendMessage(MessageUtil.toComponent("&c&lWeGuardian &8» &fEnter Custom Duration"));
        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&7Format examples:"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e1h &7= 1 hour"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e30m &7= 30 minutes"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e7d &7= 7 days"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e2w &7= 2 weeks"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e1M &7= 1 month"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e1y &7= 1 year"));
        staff.sendMessage(MessageUtil.toComponent("&7  &e2h30m &7= 2 hours 30 minutes"));
        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&7Type your duration, or type &ccancel &7to cancel."));
        staff.sendMessage(MessageUtil.toComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        plugin.getSchedulerManager().runAsyncLater(this::handleTimeout, 60, TimeUnit.SECONDS);
    }


    private void handleTimeout() {
        if (!completed) {
            cleanup();
            plugin.getSchedulerManager().runForEntity(staff,
                    () -> staff.sendMessage(MessageUtil.toComponent("&cDuration input cancelled (timed out).")));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncChat(AsyncChatEvent event) {
        if (!event.getPlayer().getUniqueId().equals(staff.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        String message = PLAIN_SERIALIZER.serialize(event.message());

        if (message.equalsIgnoreCase("cancel")) {
            cleanup();
            staff.sendMessage(MessageUtil.toComponent("&cDuration input cancelled."));
            PunishmentGUI.openAsync(plugin, staff, target);
            return;
        }

        long durationMs = TimeUtil.parseDuration(message);
        if (durationMs <= 0) {
            staff.sendMessage(MessageUtil.toComponent("&cInvalid duration format! Try again or type &ecancel&c."));
            return;
        }

        completed = true;
        cleanup();

        staff.sendMessage(MessageUtil.toComponent("&aDuration set to: &f" + TimeUtil.formatDuration(durationMs)));

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
