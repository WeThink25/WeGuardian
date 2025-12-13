package me.wethink.weguardian.listeners;

import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.util.MessageUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class PlayerLoginListener implements Listener {

    private final WeGuardian plugin;

    public PlayerLoginListener(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPreLogin(AsyncPlayerPreLoginEvent event) {
        UUID uuid = event.getUniqueId();
        String name = event.getName();
        String ipAddress = event.getAddress().getHostAddress();


        try {
            Optional<Punishment> activeBan = plugin.getPunishmentManager()
                    .getActiveBan(uuid)
                    .get(5, TimeUnit.SECONDS);

            if (activeBan.isPresent()) {
                Punishment punishment = activeBan.get();

                if (!punishment.isExpired()) {
                    String kickMessage = plugin.getPunishmentManager()
                            .buildKickMessage(punishment.getType(), punishment.getReason(), punishment.getExpiresAt());

                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            MessageUtil.toComponent(kickMessage));

                    plugin.getLogger().info(name + " attempted to join but is banned.");
                    return;
                } else {
                    plugin.getCacheManager().invalidateBan(uuid);
                }
            }

            Optional<Punishment> activeIpBan = plugin.getPunishmentManager()
                    .getActiveIpBan(ipAddress)
                    .get(5, TimeUnit.SECONDS);

            if (activeIpBan.isPresent()) {
                Punishment punishment = activeIpBan.get();

                if (!punishment.isExpired()) {
                    String kickMessage = plugin.getPunishmentManager()
                            .buildKickMessage(punishment.getType(), punishment.getReason(), punishment.getExpiresAt());

                    event.disallow(
                            AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                            MessageUtil.toComponent(kickMessage));

                    plugin.getLogger().info(name + " (" + ipAddress + ") attempted to join but their IP is banned.");
                    return;
                } else {
                    plugin.getCacheManager().invalidateIpBan(ipAddress);
                }
            }

        } catch (InterruptedException | ExecutionException e) {
            plugin.getLogger().warning("Failed to check ban status for " + name + ": " + e.getMessage());
        } catch (TimeoutException e) {
            plugin.getLogger().warning("Ban check timed out for " + name + ", allowing login.");
        }
    }
}
