package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public class KickMenu extends MenuHandler {

    private final MenuManager menuManager;

    public KickMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "kick");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("kick");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");

        plugin.getLogger().info("KickMenu: Player " + staff.getName() + " clicked item " + itemKey + " with action: " + action);

        if (action.startsWith("execute_punishment:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 3) {
                String punishmentType = parts[1];
                String reason = parts[2];
                executeKick(staff, targetPlayer, reason);
                return;
            }
        }

        if (action.startsWith("open_menu:")) {
            String menuType = action.substring("open_menu:".length());
            if (menuType.equals("main")) {
                menuManager.setActiveMenu(staff, menuManager.getMainMenu());
                menuManager.getMainMenu().openMenu(staff, targetPlayer);
                return;
            }
        }

        if (action.equals("back") || itemKey.equals("back")) {
            menuManager.setActiveMenu(staff, menuManager.getMainMenu());
            menuManager.getMainMenu().openMenu(staff, targetPlayer);
            return;
        }

        if (action.equals("close") || itemKey.equals("close")) {
            staff.closeInventory();
            return;
        }

        if (action.startsWith("custom_reason:")) {
            staff.sendMessage(MessageUtils.colorize("&eCustom input feature coming in next update!"));
            staff.sendMessage(MessageUtils.colorize("&7For now, please use the predefined options."));
            return;
        }

        if (itemConfig.contains("reason")) {
            String reason = itemConfig.getString("reason", "No reason specified");
            executeKick(staff, targetPlayer, reason);
            return;
        }

        String reason = itemConfig.getString("reason", "No reason specified");
        executeKick(staff, targetPlayer, reason);
    }

    private void executeKick(Player staff, String targetPlayer, String reason) {
        staff.closeInventory();
        clearSelectedData(staff);

        staff.sendMessage(MessageUtils.colorize("&eExecuting kick on &a" + targetPlayer + " &efor: &f" + reason));

        plugin.getPunishmentService().kick(targetPlayer, staff.getName(), reason)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully kicked " + targetPlayer + " for: &e" + reason));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to kick " + targetPlayer + ". Player may not be online."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing kick: " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the kick."));
                    return null;
                });
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        plugin.getLogger().info("Staff " + staff.getName() + " opened kick menu for " + targetPlayer);
    }
}
