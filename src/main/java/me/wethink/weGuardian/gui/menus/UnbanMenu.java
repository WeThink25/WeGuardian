package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.gui.menus.MenuManager;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class UnbanMenu extends MenuHandler {

    private final MenuManager menuManager;

    public UnbanMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "unban");
        this.menuManager = menuManager;
    }

    protected MenuManager getMenuManager() {
        return menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("unban");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");

        if (action.startsWith("execute_punishment:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 2) {
                executeUnban(staff, targetPlayer);
                return;
            }
        }

        if (action.equals("back") || itemKey.equals("back")) {
            getMenuManager().setActiveMenu(staff, getMenuManager().getMainMenu());
            getMenuManager().getMainMenu().openMenu(staff, targetPlayer);
            return;
        }

        if (action.equals("close") || itemKey.equals("close")) {
            staff.closeInventory();
            return;
        }

        if (action.equals("confirm") || itemKey.equals("confirm") || itemKey.equals("unban")) {
            executeUnban(staff, targetPlayer);
            return;
        }

        executeUnban(staff, targetPlayer);
    }

    private void executeUnban(Player staff, String targetPlayer) {
        staff.closeInventory();
        clearSelectedData(staff);

        staff.sendMessage(MessageUtils.colorize("&eExecuting unban on &a" + targetPlayer));

        plugin.getPunishmentService().unban(targetPlayer, staff.getName())
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully unbanned " + targetPlayer));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to unban " + targetPlayer + ". Player may not be banned."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing unban: " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the unban."));
                    return null;
                });
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.debug("Staff %s opened unban menu for %s", staff.getName(), targetPlayer);
        }
    }
}
