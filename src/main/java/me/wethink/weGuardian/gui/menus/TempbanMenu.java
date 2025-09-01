package me.wethink.weGuardian.gui.menus;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.MenuHandler;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.Map;

public class TempbanMenu extends MenuHandler {

    private final MenuManager menuManager;

    public TempbanMenu(WeGuardian plugin, MenuManager menuManager) {
        super(plugin, "tempban");
        this.menuManager = menuManager;
    }

    @Override
    protected ConfigurationSection getMenuConfig() {
        return plugin.getGUIConfigLoader().getGUIConfig("tempban");
    }

    @Override
    protected void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action", "");
        
        plugin.getLogger().info("TempbanMenu: Player " + staff.getName() + " clicked item " + itemKey + " with action: " + action);
        
        if (action.startsWith("set_duration:")) {
            String duration = action.substring("set_duration:".length());
            Map<String, Object> selectedData = getSelectedData(staff);
            selectedData.put("selectedDuration", duration);
            staff.sendMessage(MessageUtils.colorize("&eSelected duration: &f" + duration));
            
            refreshInventory(staff, targetPlayer);
            return;
        }
        
        if (action.startsWith("execute_punishment:")) {
            String[] parts = action.split(":", 4);
            if (parts.length >= 4) {
                String punishmentType = parts[1];
                String reason = parts[2];
                String duration = parts[3];
                
                Map<String, Object> selectedData = getSelectedData(staff);
                String selectedDuration = (String) selectedData.get("selectedDuration");
                if (selectedDuration != null && duration.contains("{selected_duration}")) {
                    duration = duration.replace("{selected_duration}", selectedDuration);
                    executeTempban(staff, targetPlayer, reason, duration);
                    return;
                } else if (!duration.contains("{selected_duration}")) {
                    executeTempban(staff, targetPlayer, reason, duration);
                    return;
                } else {
                    staff.sendMessage(MessageUtils.colorize("&cPlease select a duration first!"));
                    return;
                }
            } else if (parts.length >= 3) {
                String reason = parts[2];
                Map<String, Object> selectedData = getSelectedData(staff);
                String selectedDuration = (String) selectedData.get("selectedDuration");
                if (selectedDuration != null) {
                    executeTempban(staff, targetPlayer, reason, selectedDuration);
                    return;
                } else {
                    staff.sendMessage(MessageUtils.colorize("&cPlease select a duration first!"));
                    return;
                }
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
            Map<String, Object> selectedData = getSelectedData(staff);
            String selectedDuration = (String) selectedData.get("selectedDuration");
            if (selectedDuration != null) {
                staff.sendMessage(MessageUtils.colorize("&eCustom input feature coming in next update!"));
                staff.sendMessage(MessageUtils.colorize("&7For now, please use the predefined options."));
            } else {
                staff.sendMessage(MessageUtils.colorize("&cPlease select a duration first!"));
            }
            return;
        }

        Map<String, Object> selectedData = getSelectedData(staff);

        if (itemConfig.contains("duration")) {
            String duration = itemConfig.getString("duration", "1d");
            selectedData.put("selectedDuration", duration);
            staff.sendMessage(MessageUtils.colorize("&eSelected duration: &f" + duration));
            refreshInventory(staff, targetPlayer);
            return;
        }

        if (itemConfig.contains("reason")) {
            String reason = itemConfig.getString("reason", "No reason specified");
            String selectedDuration = (String) selectedData.get("selectedDuration");
            if (selectedDuration != null) {
                executeTempban(staff, targetPlayer, reason, selectedDuration);
            } else {
                staff.sendMessage(MessageUtils.colorize("&cPlease select a duration first!"));
            }
            return;
        }
    }

    private void refreshInventory(Player staff, String targetPlayer) {
        staff.closeInventory();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            openMenu(staff, targetPlayer);
        }, 1L);
    }

    private void executeTempban(Player staff, String targetPlayer, String reason, String duration) {
        staff.closeInventory();
        clearSelectedData(staff);

        staff.sendMessage(MessageUtils.colorize("&eExecuting tempban on &a" + targetPlayer + " &efor " + duration + ": &f" + reason));

        plugin.getPunishmentService().tempban(targetPlayer, staff.getName(), reason, duration)
                .thenAccept(success -> {
                    if (success) {
                        staff.sendMessage(MessageUtils.colorize("&aSuccessfully tempbanned " + targetPlayer + " for " + duration + ": &e" + reason));
                    } else {
                        staff.sendMessage(MessageUtils.colorize("&cFailed to tempban " + targetPlayer + ". Player may already be banned."));
                    }
                })
                .exceptionally(throwable -> {
                    plugin.getLogger().severe("Error executing tempban: " + throwable.getMessage());
                    staff.sendMessage(MessageUtils.colorize("&cAn error occurred while executing the tempban."));
                    return null;
                });
    }

    @Override
    protected void onMenuOpen(Player staff, String targetPlayer) {
        plugin.getLogger().info("Staff " + staff.getName() + " opened tempban menu for " + targetPlayer);
    }
}
