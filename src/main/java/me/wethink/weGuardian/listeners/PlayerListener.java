package me.wethink.weGuardian.listeners;

import com.tcoded.folialib.FoliaLib;
import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.PlayerData;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerListener implements Listener {

    private final WeGuardian plugin;
    private final DatabaseManager databaseManager;
    private final FoliaLib foliaLib;
    
    private final Map<String, Punishment> pendingMuteActions = new ConcurrentHashMap<>();

    public PlayerListener(WeGuardian plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.foliaLib = plugin.getFoliaLib();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLogin(AsyncPlayerPreLoginEvent event) {
        plugin.debug("Player pre-login event: %s (%s) from %s", 
                event.getName(), event.getUniqueId(), event.getAddress().getHostAddress());
        
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            plugin.debug("Player pre-login skipped: login result not allowed - %s", event.getName());
            return;
        }

        foliaLib.getScheduler().runAsync(task -> {
            try {
                handlePlayerDataAsync(event);
                
                checkPunishmentsAsync(event);
                
                checkIPBanAsync(event);
                
            } catch (Exception e) {
                plugin.getLogger().severe("Error in async pre-login processing for " + event.getName() + ": " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    private void handlePlayerDataAsync(AsyncPlayerPreLoginEvent event) {
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
        } catch (Exception e) {
            plugin.getLogger().severe("Error handling player data for " + event.getName() + ": " + e.getMessage());
        }
    }
    
    private void checkPunishmentsAsync(AsyncPlayerPreLoginEvent event) {
        try {
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
                    
                    foliaLib.getScheduler().runNextTick(task -> {
                        event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                        plugin.getNotificationService().broadcastPunishment(punishment, "reconnect_attempt");
                    });
                    return;
                }
                
                if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMPMUTE) {
                    pendingMuteActions.put(event.getUniqueId().toString(), punishment);
                    plugin.debug("Player %s is muted, will show action bar after join", event.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking punishments for " + event.getName() + ": " + e.getMessage());
        }
    }
    
    private void checkIPBanAsync(AsyncPlayerPreLoginEvent event) {
        try {
            String playerIP = event.getAddress().getHostAddress();
            if (databaseManager.isIPBanned(playerIP).join()) {
                String ipBanMessage = plugin.getMessage("screen.ipban", 
                    "&c&lYOU ARE IP BANNED\n&7Your IP address has been banned from this server.\n&7Appeal at: &e{appeal-url}");
                String appealUrl = plugin.getAppealUrl();
                ipBanMessage = ipBanMessage.replace("{appeal-url}", appealUrl);
                
                Component kickComponent = MessageUtils.toComponent(ipBanMessage);
                
                foliaLib.getScheduler().runNextTick(task -> {
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                    if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                        plugin.getLogger().info("Blocked IP banned connection attempt from " + playerIP + " (" + event.getName() + ")");
                    }
                });
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking IP ban for " + event.getName() + ": " + e.getMessage());
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        String playerUuid = event.getPlayer().getUniqueId().toString();
        Punishment mutePunishment = pendingMuteActions.remove(playerUuid);
        
        if (mutePunishment != null) {
            foliaLib.getScheduler().runAtEntity(event.getPlayer(), task -> {
                String actionBarMessage = getMuteMessage(mutePunishment);
                if (!actionBarMessage.isEmpty()) {
                    Component component = MessageUtils.toComponent(actionBarMessage);
                    event.getPlayer().sendActionBar(component);
                }
            });
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        String playerUuid = event.getPlayer().getUniqueId().toString();
        pendingMuteActions.remove(playerUuid);
        
        foliaLib.getScheduler().runAsync(task -> {
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
