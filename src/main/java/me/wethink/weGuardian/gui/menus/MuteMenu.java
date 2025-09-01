package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public class MuteMenu extends MenuHandler {

    private final MenuManager menuManager;

    public MuteMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "mute");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("mute");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");

        plugin.getLogger().info("MuteMenu: Player " + staff.getName() + " clicked item " + itemKey + " with action: " + action);

        if (action.startsWith("execute_punishment:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 3) {
                String punishmentType = parts[1];
                String reason = parts[2];
                executeMute(staff, targetPlayer, reason);
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
            executeMute(staff, targetPlayer, reason);
            return;
        }

        String reason = itemConfig.getString("reason", "No reason specified");
        executeMute(staff, targetPlayer, reason);
    }

    private void executeMute(Player staff, String targetPlayer, String reason) {
        staff.closeInventory();
        clearSelectedData(staff);

        staff.sendMessage(MessageUtils.colorize("&eExecuting mute on &a" + targetPlayer + " &efor: &f" + reason));

        plugin.getPunishmentService().mute(targetPlayer, staff.getName(), reason)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully muted " + targetPlayer + " for: &e" + reason));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to mute " + targetPlayer + ". Player may already be muted."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing mute: " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the mute."));
                    return null;
                });
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        plugin.getLogger().info("Staff " + staff.getName() + " opened mute menu for " + targetPlayer);
    }
}
