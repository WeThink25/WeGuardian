package me.wethink.weGuardian.gui;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.gui.menus.MenuManager;
import me.wethink.weGuardian.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public abstract class MenuHandler {

    protected final WeGuardian plugin;
    protected final String menuType;
    protected final Map<Player, String> targetPlayers = new HashMap<>();
    protected final Map<Player, Map<String, Object>> selectedData = new HashMap<>();
    protected MenuManager menuManager;

    public MenuHandler(WeGuardian plugin, String menuType) {
        this.plugin = plugin;
        this.menuType = menuType;
    }

    public void setMenuManager(MenuManager menuManager) {
        this.menuManager = menuManager;
    }

    public void openMenu(Player staff, String targetPlayer) {
        ConfigurationSection config = getMenuConfig();
        if (config == null) {
            staff.sendMessage(MessageUtils.colorize("&cMenu configuration not found for " + menuType + "!"));
            return;
        }

        ConfigurationSection settings = config.getConfigurationSection("settings");
        if (settings == null) {
            settings = config;
        }

        int size = settings.getInt("size", 36);
        String title = MessageUtils.colorize(settings.getString("title", menuType + " - {player}")
                .replace("{player}", targetPlayer));

        Inventory inventory = Bukkit.createInventory(null, size, Component.text(title));

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String key : items.getKeys(false)) {
                ConfigurationSection itemConfig = items.getConfigurationSection(key);
                if (itemConfig != null) {
                    MenuItem menuItem = MenuItem.fromConfig(itemConfig, targetPlayer);
                    if (menuItem != null) {
                        addItemToInventory(inventory, menuItem, key);
                    }
                }
            }
        }

        targetPlayers.put(staff, targetPlayer);
        
        if (menuManager != null) {
            menuManager.setActiveMenu(staff, this);
        }
        
        staff.openInventory(inventory);
        onMenuOpen(staff, targetPlayer);
    }

    public void handleClick(InventoryClickEvent event, int slot) {
        event.setCancelled(true);
        
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player staff = (Player) event.getWhoClicked();
        String targetPlayer = targetPlayers.get(staff);
        
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler (" + this.getClass().getSimpleName() + "): Processing click for player " + staff.getName());
            plugin.getLogger().info("MenuHandler: Target player: " + targetPlayer);
            plugin.getLogger().info("MenuHandler: Clicked slot: " + slot);
        }
        
        if (targetPlayer == null) {
            plugin.getLogger().warning("MenuHandler: No target player found for staff " + staff.getName());
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("MenuHandler: Clicked item is null or air");
            }
            return;
        }

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler: Clicked item: " + clicked.getType());
        }

        ConfigurationSection config = getMenuConfig();
        if (config == null) {
            plugin.getLogger().warning("MenuHandler: Menu config is null");
            return;
        }

        ConfigurationSection items = config.getConfigurationSection("items");
        if (items == null) {
            plugin.getLogger().warning("MenuHandler: Items section is null");
            return;
        }

        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler: Checking " + items.getKeys(false).size() + " items for slot match");
        }

        for (String key : items.getKeys(false)) {
            ConfigurationSection itemConfig = items.getConfigurationSection(key);
            if (itemConfig != null && isSlotMatch(itemConfig, slot)) {
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                    plugin.getLogger().info("MenuHandler: Found matching item: " + key + " for slot " + slot);
                }
                handleItemClick(staff, targetPlayer, key, itemConfig);
                return;
            }
        }
        
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().warning("MenuHandler: No matching item found for slot " + slot);
        }
    }

    private void addItemToInventory(Inventory inventory, MenuItem menuItem, String key) {
        for (int slot : menuItem.getSlots()) {
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, menuItem.getItemStack());
            } else {
                plugin.getLogger().warning("Invalid slot " + slot + " for item " + key + " in menu " + menuType);
            }
        }
    }

    private boolean isSlotMatch(ConfigurationSection itemConfig, int slot) {
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler: Checking slot match for slot " + slot);
        }
        
        if (itemConfig.contains("slot")) {
            int configSlot = itemConfig.getInt("slot", -1);
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("MenuHandler: Item has single slot: " + configSlot);
            }
            if (configSlot == slot) {
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                    plugin.getLogger().info("MenuHandler: Single slot match found!");
                }
                return true;
            }
        }
        
        if (itemConfig.contains("slots")) {
            List<Integer> slots = itemConfig.getIntegerList("slots");
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("MenuHandler: Item has multiple slots: " + slots);
            }
            if (slots.contains(slot)) {
                if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                    plugin.getLogger().info("MenuHandler: Multiple slot match found!");
                }
                return true;
            }
        }
        
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler: No slot match for slot " + slot);
        }
        return false;
    }

    public void cleanupPlayer(Player player) {
        targetPlayers.remove(player);
        selectedData.remove(player);
        onMenuClose(player);
    }
    

    public void setTargetPlayer(Player staff, String targetPlayer) {
        targetPlayers.put(staff, targetPlayer);
        if (plugin.getConfig().getBoolean("debug.enabled", false)) {
            plugin.getLogger().info("MenuHandler: Set target player " + targetPlayer + " for staff " + staff.getName());
        }
    }
    

    public void preserveSelectedData(Player staff, Map<String, Object> data) {
        if (data != null && !data.isEmpty()) {
            selectedData.put(staff, new HashMap<>(data));
            if (plugin.getConfig().getBoolean("debug.enabled", false)) {
                plugin.getLogger().info("MenuHandler: Preserved selected data for staff " + staff.getName() + ": " + data);
            }
        }
    }

    public String getTargetPlayer(Player staff) {
        return targetPlayers.get(staff);
    }

    public Map<String, Object> getSelectedData(Player player) {
        return selectedData.computeIfAbsent(player, k -> new HashMap<>());
    }

    protected void clearSelectedData(Player player) {
        selectedData.remove(player);
    }

    protected abstract ConfigurationSection getMenuConfig();

    protected abstract void handleItemClick(Player staff, String targetPlayer, String itemKey, ConfigurationSection itemConfig);

    protected void onMenuOpen(Player staff, String targetPlayer) {
    }

    protected void onMenuClose(Player player) {
    }

    protected CompletableFuture<Boolean> executePunishment(String punishmentType, String targetPlayer, String staffName, String reason, String duration) {
        return plugin.getPunishmentService().executePunishment(
                me.wethink.weGuardian.models.PunishmentType.valueOf(punishmentType.toUpperCase()),
                targetPlayer, 
                staffName, 
                reason, 
                duration
        );
    }

    protected void sendSuccessMessage(Player staff, String punishmentType, String targetPlayer, String reason, String duration) {
        String message = switch (punishmentType.toLowerCase()) {
            case "ban" -> "&aSuccessfully banned " + targetPlayer + " for: &e" + reason;
            case "tempban" -> "&aSuccessfully tempbanned " + targetPlayer + " for " + duration + ": &e" + reason;
            case "mute" -> "&aSuccessfully muted " + targetPlayer + " for: &e" + reason;
            case "tempmute" -> "&aSuccessfully tempmuted " + targetPlayer + " for " + duration + ": &e" + reason;
            case "kick" -> "&aSuccessfully kicked " + targetPlayer + " for: &e" + reason;
            case "warn" -> "&aSuccessfully warned " + targetPlayer + " for: &e" + reason;
            default -> "&aSuccessfully punished " + targetPlayer + " for: &e" + reason;
        };
        staff.sendMessage(MessageUtils.colorize(message));
    }

    protected void sendErrorMessage(Player staff, String punishmentType, String targetPlayer) {
        String message = switch (punishmentType.toLowerCase()) {
            case "ban", "tempban" -> "&cFailed to ban " + targetPlayer + ". Player may already be banned.";
            case "mute", "tempmute" -> "&cFailed to mute " + targetPlayer + ". Player may already be muted.";
            case "kick" -> "&cFailed to kick " + targetPlayer + ". Player may not be online.";
            case "warn" -> "&cFailed to warn " + targetPlayer + ".";
            default -> "&cFailed to punish " + targetPlayer + ".";
        };
        staff.sendMessage(MessageUtils.colorize(message));
    }
}
