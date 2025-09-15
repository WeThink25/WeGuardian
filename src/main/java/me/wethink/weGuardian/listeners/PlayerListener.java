package me.wethink.weGuardian.listeners;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PlayerListener implements Listener {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;

    public PlayerListener(WeGuardian plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.debug("Player pre-login event: %s (%s) from %s", 
                event.getName(), event.getUniqueId(), event.getAddress().getHostAddress());
        
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.debug("Player pre-login skipped: login result not allowed - %s", event.getName());
            return;
        }

        try {
            PlayerData playerData = databaseManager.getPlayerData(event.getUniqueId()).join();
            if (playerData == null) {
                plugin.debug("Creating new player data for: %s", event.getName());
                playerData = new PlayerData(event.getUniqueId(), event.getName(), event.getAddress().getHostAddress());
                databaseManager.savePlayerData(playerData);
            } else {
                plugin.debug("Updating existing player data for: %s", event.getName());
                playerData.setName(event.getName());
                playerData.setIpAddress(event.getAddress().getHostAddress());
                playerData.updateLastSeen();
                databaseManager.savePlayerData(playerData);
            }

            databaseManager.recordPlayerConnection(
                    event.getUniqueId(),
                    event.getName(),
                    event.getAddress().getHostAddress()
            );

            List<Punishment> punishments = databaseManager.getActivePunishments(event.getUniqueId()).join();
            plugin.debug("Checking %d active punishments for player: %s", punishments.size(), event.getName());
            
            for (Punishment punishment : punishments) {
                if (punishment.isExpired()) {
                    plugin.debug("Skipping expired punishment: %s for %s", punishment.getType(), event.getName());
                    continue;
                }

                if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) {
                    plugin.debug("Player %s is banned, blocking connection", event.getName());
                    String kickMessage = getBanMessage(punishment);
                    Component kickComponent = MessageUtils.toComponent(kickMessage);
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                    plugin.getNotificationService().broadcastPunishment(punishment, "reconnect_attempt");
                    return;
                }
            }

            String playerIP = event.getAddress().getHostAddress();
            if (databaseManager.isIPBanned(playerIP).join()) {
                String ipBanMessage = plugin.getMessage("screen.ipban", 
                    "&c&lYOU ARE IP BANNED\n&7Your IP address has been banned from this server.\n&7Appeal at: &e{appeal-url}");
                String appealUrl = plugin.getAppealUrl();
                ipBanMessage = ipBanMessage.replace("{appeal-url}", appealUrl);
                
                Component kickComponent = MessageUtils.toComponent(ipBanMessage);
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                    plugin.getLogger().info("Blocked IP banned connection attempt from " + playerIP + " (" + event.getName() + ")");
                }
                return;
            }

        } catch (Exception e) {
            plugin.getLogger().severe("Error checking ban status for " + event.getName() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @EventHandler
    public void onPlayeJoin(PlayerJoinEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                List<Punishment> punishments = databaseManager.getActivePunishments(event.getPlayer().getUniqueId()).join();
                for (Punishment punishment : punishments) {
                    if (punishment.isExpired()) {
                        continue;
                    }

                    if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMPMUTE) {
                        String actionBarMessage = getMuteMessage(punishment);
                        if (!actionBarMessage.isEmpty()) {
                            Component component = MessageUtils.toComponent(actionBarMessage);
                            plugin.getServer().getScheduler().runTask(plugin, () -> {
                                Player player = event.getPlayer();
                                if (player.isOnline()) {
                                    player.sendActionBar(component);
                                }
                            });
                        }
                        break;
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error checking mute status for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        CompletableFuture.runAsync(() -> {
            try {
                PlayerData playerData = databaseManager.getPlayerData(event.getPlayer().getUniqueId()).join();
                if (playerData != null) {
                    playerData.updateLastSeen();
                    databaseManager.savePlayerData(playerData);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error updating last seen for " + event.getPlayer().getName() + ": " + e.getMessage());
            }
        });

        plugin.getPunishmentGUI().cleanupPlayer(event.getPlayer());
    }

    private String getBanMessage(Punishment punishment) {
        String template = switch (punishment.getType()) {
            case BAN -> plugin.getMessage("screen.ban", "&c&lYOU ARE BANNED");
            case TEMPBAN -> plugin.getMessage("screen.tempban", "&e&lTEMPORARY BAN");
            default -> "&cYou are banned from this server.";
        };

        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    private String getMuteMessage(Punishment punishment) {
        String key = "actionbar." + punishment.getType().name().toLowerCase();
        String template = plugin.getMessage(key, "");
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }
}
