package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.gui.MenuItem;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;


public class NotesMenu extends MenuHandler {

    private final MenuManager menuManager;

    public NotesMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "notes");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("notes");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        MenuItem menuItem = MenuItem.fromConfig(itemConfig, targetPlayer);
        if (menuItem == null) return;

        if (menuItem.isBackAction()) {
            menuManager.setActiveMenu(staff, menuManager.getMainMenu());
            menuManager.getMainMenu().openMenu(staff, targetPlayer);
            return;
        }

        if (menuItem.isCloseAction()) {
            staff.closeInventory();
            return;
        }

        staff.sendMessage(MessageUtils.colorize("&cNotes system coming in next update!"));
        staff.sendMessage(MessageUtils.colorize("&7This feature will allow you to add, view, and manage player notes."));
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        plugin.debug("Staff %s opened notes menu for %s", staff.getName(), targetPlayer);
        staff.sendMessage(MessageUtils.colorize("&eNotes system is under development!"));
    }
}
