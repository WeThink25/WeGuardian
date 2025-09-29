package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.*;
import java.util.regex.Pattern;

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

    private static final Pattern COLOR_CODES = Pattern.compile("§[0-9a-fk-or]");
    private static final Set<String> KNOWN_MENU_TITLES = Set.of(
            "Ban Menu", "Mute Menu", "Kick Menu", "Warn Menu",
            "Tempban Menu", "Tempmute Menu", "Unban Menu",
            "Unmute Menu", "Notes Menu", "Punishment"
    );

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
        if (!(event.getWhoClicked() instanceof Player player)) return;

        MenuHandler activeMenu = activeMenus.get(player);
        if (activeMenu != null) {
            event.setCancelled(true);
            activeMenu.handleClick(event, event.getSlot());
            return;
        }

        Component componentTitle = event.getView().title();
        String plainTitle = PlainTextComponentSerializer.plainText().serialize(componentTitle);
        String strippedTitle = COLOR_CODES.matcher(plainTitle).replaceAll("");

        if (isWeGuardianGUI(strippedTitle)) {
            event.setCancelled(true);
            MenuHandler recoveredMenu = findMenuByTitle(strippedTitle);
            if (recoveredMenu != null) {
                String targetPlayer = extractTargetPlayerFromTitle(strippedTitle);
                if (targetPlayer != null) {
                    recoveredMenu.setTargetPlayer(player, targetPlayer);
                }
                activeMenus.put(player, recoveredMenu);
                recoveredMenu.handleClick(event, event.getSlot());
            } else {
                plugin.debug("Failed to recover WeGuardian menu with title: %s", strippedTitle);
            }
        }
    }

    private boolean isWeGuardianGUI(String title) {
        for (String keyword : KNOWN_MENU_TITLES) {
            if (title.contains(keyword)) return true;
        }
        return false;
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
        if (event.getPlayer() instanceof Player player) {
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
