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

        try {
            checkPunishmentsSync(event);
            
            if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                checkIPBanSync(event);
            }
            
            if (event.getLoginResult() == AsyncPlayerPreLoginEvent.Result.ALLOWED) {
                foliaLib.getScheduler().runAsync(task -> {
                    try {
                        handlePlayerDataAsync(event);
                    } catch (Exception e) {
                        plugin.getLogger().severe("Error handling player data for " + event.getName() + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                });
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Critical error in pre-login processing for " + event.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                MessageUtils.toComponent("&c&lCONNECTION ERROR\n&7An error occurred during login verification.\n&7Please try again later."));
        }
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
    
    private void checkPunishmentsSync(AsyncPlayerPreLoginEvent event) {
        try {
            Boolean isBanned = databaseManager.isPlayerBanned(event.getUniqueId()).join();
            plugin.debug("Direct ban check for %s: %s", event.getName(), isBanned);
            
            if (isBanned != null && isBanned) {
                plugin.debug("Player %s is banned (direct check), blocking connection", event.getName());
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, 
                    MessageUtils.toComponent("&c&lYOU ARE BANNED\n&7You have been banned from this server."));
                return;
            }
            
            List<Punishment> punishments = databaseManager.getActivePunishments(event.getUniqueId()).join();
            plugin.debug("Checking %d active punishments for player: %s", punishments.size(), event.getName());
            
            if (punishments == null) {
                plugin.getLogger().warning("Database returned null punishments for " + event.getName() + " - denying access for safety");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                    MessageUtils.toComponent("&c&lCONNECTION ERROR\n&7Unable to verify your punishment status.\n&7Please try again later."));
                return;
            }
            
            for (Punishment punishment : punishments) {
                if (punishment == null) {
                    plugin.debug("Skipping null punishment for %s", event.getName());
                    continue;
                }
                
                plugin.debug("Found punishment: type=%s, active=%s, expired=%s, target=%s", 
                    punishment.getType(), punishment.isActive(), punishment.isExpired(), punishment.getTargetName());

                if (punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN) {
                    plugin.debug("Player %s is banned, blocking connection", event.getName());
                    String kickMessage = getBanMessage(punishment);
                    Component kickComponent = MessageUtils.toComponent(kickMessage);
                    
                    event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                    plugin.getNotificationService().broadcastPunishment(punishment, "reconnect_attempt");
                    return;
                }
                
                if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.TEMPMUTE) {
                    pendingMuteActions.put(event.getUniqueId().toString(), punishment);
                    plugin.debug("Player %s is muted, will show action bar after join", event.getName());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking punishments for " + event.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            plugin.getLogger().warning("Denying access to " + event.getName() + " due to punishment check failure");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                MessageUtils.toComponent("&c&lCONNECTION ERROR\n&7Unable to verify your punishment status.\n&7Please try again later."));
        }
    }
    
    private void checkIPBanSync(AsyncPlayerPreLoginEvent event) {
        try {
            String playerIP = event.getAddress().getHostAddress();
            Boolean isIPBanned = databaseManager.isIPBanned(playerIP).join();
            
            if (isIPBanned == null) {
                plugin.getLogger().warning("Database returned null for IP ban check for " + event.getName() + " (" + playerIP + ") - denying access for safety");
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                    MessageUtils.toComponent("&c&lCONNECTION ERROR\n&7Unable to verify IP ban status.\n&7Please try again later."));
                return;
            }
            
            if (isIPBanned) {
                String ipBanMessage = plugin.getMessage("screen.ipban", 
                    "&c&lYOU ARE IP BANNED\n&7Your IP address has been banned from this server.\n&7Appeal at: &e{appeal-url}");
                String appealUrl = plugin.getAppealUrl();
                ipBanMessage = ipBanMessage.replace("{appeal-url}", appealUrl);
                
                Component kickComponent = MessageUtils.toComponent(ipBanMessage);
                
                event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, kickComponent);
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                    plugin.getLogger().info("Blocked IP banned connection attempt from " + playerIP + " (" + event.getName() + ")");
                }
            }
        } catch (Exception e) {
            plugin.getLogger().severe("Error checking IP ban for " + event.getName() + ": " + e.getMessage());
            e.printStackTrace();
            
            plugin.getLogger().warning("Denying access to " + event.getName() + " due to IP ban check failure");
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, 
                MessageUtils.toComponent("&c&lCONNECTION ERROR\n&7Unable to verify IP ban status.\n&7Please try again later."));
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
