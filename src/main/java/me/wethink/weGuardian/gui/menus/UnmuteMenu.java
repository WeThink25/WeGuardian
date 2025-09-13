package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class UnmuteMenu extends MenuHandler {

    private final MenuManager menuManager;

    public UnmuteMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "unmute");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("unmute");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");
        
        if (action.startsWith("execute_punishment:")) {
            String[] parts = action.split(":", 3);
            if (parts.length >= 2) {
                executeUnmute(staff, targetPlayer);
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

        if (action.equals("confirm") || itemKey.equals("confirm") || itemKey.equals("unmute")) {
            executeUnmute(staff, targetPlayer);
            return;
        }

        executeUnmute(staff, targetPlayer);
    }

    private void executeUnmute(Player staff, String targetPlayer) {
        staff.closeInventory();
        clearSelectedData(staff);

        staff.sendMessage(MessageUtils.colorize("&eExecuting unmute on &a" + targetPlayer));

        plugin.getPunishmentService().unmute(targetPlayer, staff.getName())
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully unmuted " + targetPlayer));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to unmute " + targetPlayer + ". Player may not be muted."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing unmute: " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the unmute."));
                    return null;
                });
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.debug("Staff %s opened unmute menu for %s", staff.getName(), targetPlayer);
        }
    }
}
