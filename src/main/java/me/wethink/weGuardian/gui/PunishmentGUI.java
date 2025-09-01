package me.wethink.weGuardian.gui;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.menus.MenuManager;
import org.bukkit.entity.Player;


public class PunishmentGUI {

    private final WeGuardian plugin;
    private final MenuManager menuManager;

    public PunishmentGUI(WeGuardian plugin) {
        this.plugin = plugin;
        this.menuManager = new MenuManager(plugin);
    }


    public void openMainGUI(Player staff, String targetPlayer) {
        menuManager.openMainMenu(staff, targetPlayer);
    }


    public void openPunishmentGUI(Player staff, String targetPlayer, String punishmentType) {
        menuManager.openMenu(staff, targetPlayer, punishmentType);
    }


    public void openUnbanGUI(Player staff, String targetPlayer) {
        menuManager.openMenu(staff, targetPlayer, "unban");
    }


    public void openUnmuteGUI(Player staff, String targetPlayer) {
        menuManager.openMenu(staff, targetPlayer, "unmute");
    }


    public void cleanupPlayer(Player player) {
        menuManager.cleanupPlayer(player);
    }


    public void reloadGUIConfigs() {
        plugin.getGUIConfigLoader().reloadAllGUIConfigs();
    }


    public MenuManager getMenuManager() {
        return menuManager;
    }
}
