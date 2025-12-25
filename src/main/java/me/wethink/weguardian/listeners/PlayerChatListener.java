package me.wethink.weguardian.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.util.MessageUtil;
import me.wethink.weguardian.util.TimeUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.net.InetSocketAddress;
import java.util.Optional;
import java.util.UUID;

public class PlayerChatListener implements Listener {

    private final WeGuardian plugin;

    public PlayerChatListener(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onAsyncChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        String bypassPerm = plugin.getConfig().getString("bypass.permission", "weguardian.bypass");
        if (player.hasPermission(bypassPerm)) {
            return;
        }

        String ipAddress = getPlayerIp(player);

        Optional<Punishment> activeMute = plugin.getCacheManager().getActiveMute(uuid);

        if (activeMute != null && activeMute.isPresent()) {
            Punishment punishment = activeMute.get();

            if (!punishment.isExpired()) {
                handleMute(event, player, punishment);
                return;
            } else {
                plugin.getCacheManager().invalidateMute(uuid);
            }
        }

        if (ipAddress != null) {
            Optional<Punishment> activeIpMute = plugin.getCacheManager().getActiveIpMute(ipAddress);

            if (activeIpMute != null && activeIpMute.isPresent()) {
                Punishment punishment = activeIpMute.get();

                if (!punishment.isExpired()) {
                    handleMute(event, player, punishment);
                    return;
                } else {
                    plugin.getCacheManager().invalidateIpMute(ipAddress);
                }
            }
        }
    }

    private void handleMute(AsyncChatEvent event, Player player, Punishment punishment) {
        event.setCancelled(true);
        event.viewers().clear();
        String muteMessage = plugin.getMessagesManager().getMessage("punishments.mute.blocked",
                "{reason}", punishment.getReason() != null ? punishment.getReason() : "No reason specified",
                "{expires}", TimeUtil.formatRemaining(punishment.getExpiresAt()),
                "{player}", punishment.getTargetName() != null ? punishment.getTargetName() : player.getName(),
                "{staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "Console");

        player.sendMessage(MessageUtil.toComponent(muteMessage));
    }

    private String getPlayerIp(Player player) {
        InetSocketAddress address = player.getAddress();
        if (address == null) {
            return null;
        }
        String ip = address.getAddress().getHostAddress();
        if (ip != null && ip.startsWith("::ffff:")) {
            return ip.substring(7);
        }
        return ip;
    }
}
