package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;

public class MenuManager implements Listener {

    private final WeGuardian plugin;
    private final Map<Player, MenuHandler> activeMenus = new HashMap<>();

    private final MainMenu mainMenu;
    private final BanMenu banMenu;
    private final TempbanMenu tempbanMenu;
    private final MuteMenu muteMenu;
    private final TempmuteMenu tempmuteMenu;
    private final KickMenu kickMenu;
    private final WarnMenu warnMenu;
    private final NotesMenu notesMenu;
    private final UnbanMenu unbanMenu;
    private final UnmuteMenu unmuteMenu;

    public MenuManager(WeGuardian plugin) {
        this.plugin = plugin;
        
        this.mainMenu = new MainMenu(plugin, this);
        this.banMenu = new BanMenu(plugin, this);
        this.tempbanMenu = new TempbanMenu(plugin, this);
        this.muteMenu = new MuteMenu(plugin, this);
        this.tempmuteMenu = new TempmuteMenu(plugin, this);
        this.kickMenu = new KickMenu(plugin, this);
        this.warnMenu = new WarnMenu(plugin, this);
        this.notesMenu = new NotesMenu(plugin, this);
        this.unbanMenu = new UnbanMenu(plugin, this);
        this.unmuteMenu = new UnmuteMenu(plugin, this);

        this.mainMenu.setMenuManager(this);
        this.banMenu.setMenuManager(this);
        this.tempbanMenu.setMenuManager(this);
        this.muteMenu.setMenuManager(this);
        this.tempmuteMenu.setMenuManager(this);
        this.kickMenu.setMenuManager(this);
        this.warnMenu.setMenuManager(this);
        this.notesMenu.setMenuManager(this);
        this.unbanMenu.setMenuManager(this);
        this.unmuteMenu.setMenuManager(this);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void openMainMenu(Player staff, String targetPlayer) {
        activeMenus.put(staff, mainMenu);
        mainMenu.openMenu(staff, targetPlayer);
    }

    public void openMenu(Player staff, String targetPlayer, String menuType) {
        MenuHandler menu = getMenuByType(menuType);
        if (menu != null) {
            activeMenus.put(staff, menu);
            menu.openMenu(staff, targetPlayer);
        } else {
            staff.sendMessage("§cUnknown menu type: " + menuType);
        }
    }

    private MenuHandler getMenuByType(String menuType) {
        return switch (menuType.toLowerCase()) {
            case "main" -> mainMenu;
            case "ban" -> banMenu;
            case "tempban" -> tempbanMenu;
            case "mute" -> muteMenu;
            case "tempmute" -> tempmuteMenu;
            case "kick" -> kickMenu;
            case "warn" -> warnMenu;
            case "notes" -> notesMenu;
            case "unban" -> unbanMenu;
            case "unmute" -> unmuteMenu;
            default -> null;
        };
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        MenuHandler activeMenu = activeMenus.get(player);
        
        String title = event.getView().title().toString();
        String strippedTitle = title.replaceAll("§[0-9a-fk-or]", "");
        
        plugin.getLogger().info("MenuManager: Player " + player.getName() + " clicked slot " + event.getSlot() + " in inventory: " + title);
        plugin.getLogger().info("MenuManager: Active menu for player: " + (activeMenu != null ? activeMenu.getClass().getSimpleName() : "null"));
        plugin.getLogger().info("MenuManager: Stripped title: " + strippedTitle);
        
        if (activeMenu != null) {
            event.setCancelled(true);
            plugin.getLogger().info("MenuManager: Calling handleClick on active menu: " + activeMenu.getClass().getSimpleName());
            
            activeMenu.handleClick(event, event.getSlot());
        } else {
            boolean isWeGuardianGUI = title.contains("Ban Menu") || title.contains("Mute Menu") ||
                title.contains("Kick Menu") || title.contains("Warn Menu") || 
                title.contains("Tempban Menu") || title.contains("Tempmute Menu") ||
                title.contains("Unban Menu") || title.contains("Unmute Menu") || 
                title.contains("Notes Menu") || title.contains("Punishment") ||
                strippedTitle.contains("Ban Menu") || strippedTitle.contains("Mute Menu") || 
                strippedTitle.contains("Kick Menu") || strippedTitle.contains("Warn Menu") || 
                strippedTitle.contains("Tempban Menu") || strippedTitle.contains("Tempmute Menu") ||
                strippedTitle.contains("Unban Menu") || strippedTitle.contains("Unmute Menu") || 
                strippedTitle.contains("Notes Menu") || strippedTitle.contains("Punishment");
                
            plugin.getLogger().info("MenuManager: Is WeGuardian GUI: " + isWeGuardianGUI);
                
            if (isWeGuardianGUI) {
                event.setCancelled(true);
                
                MenuHandler recoveredMenu = findMenuByTitle(strippedTitle);
                if (recoveredMenu != null) {
                    if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                        plugin.getLogger().info("MenuManager: Recovered menu: " + recoveredMenu.getClass().getSimpleName());
                    }
                    
                    String targetPlayer = extractTargetPlayerFromTitle(strippedTitle);
                    if (targetPlayer != null) {
                        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                            plugin.getLogger().info("MenuManager: Extracted target player from title: " + targetPlayer);
                        }
                        recoveredMenu.setTargetPlayer(player, targetPlayer);
                        
                        for (MenuHandler existingMenu : activeMenus.values()) {
                            if (existingMenu.getClass().equals(recoveredMenu.getClass())) {
                                Map<String, Object> existingData = existingMenu.getSelectedData(player);
                                if (existingData != null && !existingData.isEmpty()) {
                                    recoveredMenu.preserveSelectedData(player, existingData);
                                    break;
                                }
                            }
                        }
                    } else {
                        plugin.getLogger().warning("MenuManager: Could not extract target player from title: " + strippedTitle);
                    }
                    
                    activeMenus.put(player, recoveredMenu);
                    if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                        plugin.getLogger().info("Recovered untracked menu for player " + player.getName() + ": " + title);
                        plugin.getLogger().info("MenuManager: Calling handleClick on recovered menu: " + recoveredMenu.getClass().getSimpleName());
                    }
                    recoveredMenu.handleClick(event, event.getSlot());
                    return;
                } else {
                    if (isWeGuardianGUI) {
                        plugin.getLogger().warning("Failed to recover WeGuardian menu with title: " + strippedTitle);
                        plugin.getLogger().warning("This indicates a menu tracking issue. Active menus: " + activeMenus.size());
                        plugin.getLogger().warning("Stripped title: " + strippedTitle);
                    }
                }
            }
        }
    }
    
    private MenuHandler findMenuByTitle(String strippedTitle) {
        if (strippedTitle.contains("Ban Menu")) return banMenu;
        if (strippedTitle.contains("Tempban Menu")) return tempbanMenu;
        if (strippedTitle.contains("Mute Menu")) return muteMenu;
        if (strippedTitle.contains("Tempmute Menu")) return tempmuteMenu;
        if (strippedTitle.contains("Kick Menu")) return kickMenu;
        if (strippedTitle.contains("Warn Menu")) return warnMenu;
        if (strippedTitle.contains("Unban Menu")) return unbanMenu;
        if (strippedTitle.contains("Unmute Menu")) return unmuteMenu;
        if (strippedTitle.contains("Notes Menu")) return notesMenu;
        if (strippedTitle.contains("Punishment")) return mainMenu;
        return null;
    }

    private String extractTargetPlayerFromTitle(String strippedTitle) {
        int pipeIndex = strippedTitle.lastIndexOf(" | ");
        if (pipeIndex != -1 && pipeIndex < strippedTitle.length() - 3) {
            String extracted = strippedTitle.substring(pipeIndex + 3).trim();
            String[] parts = extracted.split("[^a-zA-Z0-9_]");
            return parts.length > 0 ? parts[0] : null;
        }
        
        int dashIndex = strippedTitle.lastIndexOf(" - ");
        if (dashIndex != -1 && dashIndex < strippedTitle.length() - 3) {
            String extracted = strippedTitle.substring(dashIndex + 3).trim();
            String[] parts = extracted.split("[^a-zA-Z0-9_]");
            return parts.length > 0 ? parts[0] : null;
        }
        
        return null;
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getPlayer() instanceof Player) {
            Player player = (Player) event.getPlayer();
            MenuHandler activeMenu = activeMenus.remove(player);
            
            if (activeMenu != null) {
                activeMenu.cleanupPlayer(player);
            }
        }
    }

    public void setActiveMenu(Player player, MenuHandler menu) {
        activeMenus.put(player, menu);
    }

    public void cleanupPlayer(Player player) {
        MenuHandler activeMenu = activeMenus.remove(player);
        if (activeMenu != null) {
            activeMenu.cleanupPlayer(player);
        }
    }

    public MainMenu getMainMenu() { return mainMenu; }
    public BanMenu getBanMenu() { return banMenu; }
    public TempbanMenu getTempbanMenu() { return tempbanMenu; }
    public MuteMenu getMuteMenu() { return muteMenu; }
    public TempmuteMenu getTempmuteMenu() { return tempmuteMenu; }
    public KickMenu getKickMenu() { return kickMenu; }
    public WarnMenu getWarnMenu() { return warnMenu; }
    public NotesMenu getNotesMenu() { return notesMenu; }
    public UnbanMenu getUnbanMenu() { return unbanMenu; }
    public UnmuteMenu getUnmuteMenu() { return unmuteMenu; }
}
