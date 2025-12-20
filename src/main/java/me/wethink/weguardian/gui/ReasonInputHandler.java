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

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ReasonInputHandler implements Listener {

    private static final PlainTextComponentSerializer PLAIN_SERIALIZER = PlainTextComponentSerializer.plainText();

    private final WeGuardian plugin;
    private final Player staff;
    private final OfflinePlayer target;
    private final PunishmentType type;
    private final long durationMs;
    private volatile boolean completed = false;

    public ReasonInputHandler(WeGuardian plugin, Player staff, OfflinePlayer target,
            PunishmentType type, long durationMs) {
        this.plugin = plugin;
        this.staff = staff;
        this.target = target;
        this.type = type;
        this.durationMs = durationMs;
    }

    public void start() {
        if (!staff.hasPermission(type.getPermission())) {
            staff.sendMessage(MessageUtil.toComponent("&cYou don't have permission to use this punishment!"));
            return;
        }

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        String typeDisplay = type.getColor() + type.getDisplayName();
        String durationDisplay = durationMs > 0 ? TimeUtil.formatDuration(durationMs) : "Permanent";

        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));
        staff.sendMessage(MessageUtil.toComponent("&c&lWeGuardian &8» &fEnter Punishment Reason"));
        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&7Target: &e" + target.getName()));
        staff.sendMessage(MessageUtil.toComponent("&7Type: " + typeDisplay));
        staff.sendMessage(MessageUtil.toComponent("&7Duration: &f" + durationDisplay));
        staff.sendMessage(MessageUtil.toComponent(""));
        staff.sendMessage(MessageUtil.toComponent("&7Type your reason in chat, or type &ccancel &7to cancel."));
        staff.sendMessage(MessageUtil.toComponent("&8&m━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"));

        plugin.getSchedulerManager().runAsyncLater(this::handleTimeout, 60, TimeUnit.SECONDS);
    }

    private void handleTimeout() {
        if (!completed) {
            cleanup();
            plugin.getSchedulerManager().runForEntity(staff,
                    () -> staff.sendMessage(MessageUtil.toComponent("&cPunishment cancelled (timed out).")));
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
            staff.sendMessage(MessageUtil.toComponent("&cPunishment cancelled."));
            return;
        }

        completed = true;
        cleanup();

        executePunishment(message);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (event.getPlayer().getUniqueId().equals(staff.getUniqueId())) {
            cleanup();
        }
    }

    private void executePunishment(String reason) {
        if (!staff.hasPermission(type.getPermission())) {
            plugin.getSchedulerManager().runForEntity(staff, () -> staff
                    .sendMessage(MessageUtil.toComponent("&cYou don't have permission to use this punishment!")));
            return;
        }

        UUID staffUUID = staff.getUniqueId();
        String staffName = staff.getName();

        var future = switch (type) {
            case BAN -> plugin.getPunishmentManager().ban(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, reason);
            case TEMPBAN -> plugin.getPunishmentManager().tempban(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, reason);
            case MUTE -> plugin.getPunishmentManager().mute(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, reason);
            case TEMPMUTE -> plugin.getPunishmentManager().tempmute(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, durationMs, reason);
            case KICK -> plugin.getPunishmentManager().kick(
                    target.getUniqueId(), target.getName(), staffUUID, staffName, reason);
            case BANIP, TEMPBANIP, MUTEIP, TEMPMUTEIP -> throw new UnsupportedOperationException(
                    "IP-based punishments must be executed via commands, not the GUI");
        };

        future.thenAccept(result -> {
            String durationStr = durationMs > 0 ? TimeUtil.formatDuration(durationMs) : "permanent";
            String actionVerb = type.getDisplayName().toLowerCase() + (type == PunishmentType.KICK ? "ed " : "ned ");

            String successMessage = "&aSuccessfully " + actionVerb + target.getName() + " (" + durationStr + ")";
            String prefix = plugin.getConfig().getString("messages.prefix", "&8[&c&lWeGuardian&8]&r ");
            String broadcastMessage = prefix + "&c" + staffName + " &7" + actionVerb + "&c" + target.getName()
                    + " &7for: &f" + reason;

            plugin.getSchedulerManager().runSync(() -> {
                staff.sendMessage(MessageUtil.toComponent(successMessage));

                broadcastToStaff(broadcastMessage);
            });
        }).exceptionally(e -> {
            plugin.getSchedulerManager().runForEntity(staff, () -> staff
                    .sendMessage(MessageUtil.toComponent("&cAn error occurred while applying the punishment.")));
            plugin.getLogger().severe("Failed to apply punishment: " + e.getMessage());
            return null;
        });
    }

    private void broadcastToStaff(String message) {
        Collection<? extends Player> players = plugin.getServer().getOnlinePlayers();
        for (Player p : players) {
            if (p.hasPermission("weguardian.staff")) {
                p.sendMessage(MessageUtil.toComponent(message));
            }
        }
        plugin.getServer().getConsoleSender().sendMessage(MessageUtil.toComponent(message));
    }

    private void cleanup() {
        completed = true;
        HandlerList.unregisterAll(this);
    }
}
