package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public class MenuValidationService {

    private final WeGuardian plugin;
    private final boolean validationEnabled;
    private final boolean checkMenuLinks;
    private final boolean checkItemMaterials;
    private final boolean checkActionFormats;
    private final boolean autoFixIssues;

    public MenuValidationService(WeGuardian plugin) {
        this.plugin = plugin;
        
        ConfigurationSection config = plugin.getConfig().getConfigurationSection("menus.validation");
        this.validationEnabled = config != null && config.getBoolean("enabled", true);
        this.checkMenuLinks = config != null && config.getBoolean("check_menu_links", true);
        this.checkItemMaterials = config != null && config.getBoolean("check_item_materials", true);
        this.checkActionFormats = config != null && config.getBoolean("check_action_formats", true);
        this.autoFixIssues = config != null && config.getBoolean("auto_fix_issues", false);
        
        if (validationEnabled) {
            plugin.debug("Menu validation service initialized");
            validateAllMenus();
        } else {
            plugin.debug("Menu validation disabled");
        }
    }

    public void validateAllMenus() {
        if (!validationEnabled) return;
        
        plugin.debug("Starting menu validation process");
        
        List<String> availableMenus = plugin.getConfig().getStringList("menus.available_menus");
        File guiDir = new File(plugin.getDataFolder(), "gui");
        
        if (!guiDir.exists()) {
            plugin.getLogger().warning("GUI directory not found: " + guiDir.getAbsolutePath());
            return;
        }
        
        for (String menuName : availableMenus) {
            File menuFile = new File(guiDir, menuName + ".yml");
            if (menuFile.exists()) {
                validateMenuFile(menuName, menuFile);
            } else {
                plugin.getLogger().warning("Menu file not found: " + menuFile.getName());
            }
        }
        
        plugin.debug("Menu validation process completed");
    }

    private void validateMenuFile(String menuName, File menuFile) {
        plugin.debug("Validating menu file: %s", menuFile.getName());
        
        try {
            YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);
            
            validateMenuSize(menuName, config);
            ConfigurationSection items = config.getConfigurationSection("items");
            if (items != null) {
                for (String itemKey : items.getKeys(false)) {
                    ConfigurationSection itemConfig = items.getConfigurationSection(itemKey);
                    if (itemConfig != null) {
                        validateMenuItem(menuName, itemKey, itemConfig);
                    }
                }
            } else {
                plugin.getLogger().warning("No items found in menu: " + menuName);
            }
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error validating menu file " + menuFile.getName() + ": " + e.getMessage());
            plugin.debug("Menu validation error for %s: %s", menuName, e.getMessage());
        }
    }

    private void validateMenuSize(String menuName, YamlConfiguration config) {
        int size = config.getInt("size", 0);
        if (size == 0) {
            size = config.getInt("settings.size", 0);
        }
        
        List<Integer> validSizes = plugin.getConfig().getIntegerList("menus.size_validation.valid_sizes");
        if (!validSizes.contains(size)) {
            plugin.getLogger().warning("Invalid menu size for " + menuName + ": " + size + " (must be one of: " + validSizes + ")");
            
            if (autoFixIssues) {
                int defaultSize = plugin.getConfig().getInt("menus.size_validation.default_sizes." + menuName, 45);
                config.set("size", defaultSize);
                plugin.debug("Auto-fixed menu size for %s to %d", menuName, defaultSize);
            }
        }
    }

    private void validateMenuItem(String menuName, String itemKey, ConfigurationSection itemConfig) {
        validateRequiredProperties(menuName, itemKey, itemConfig);
        
        if (checkItemMaterials) {
            validateItemMaterial(menuName, itemKey, itemConfig);
        }
        
        if (checkActionFormats) {
            validateItemAction(menuName, itemKey, itemConfig);
        }
        if (checkMenuLinks) {
            validateMenuLinks(menuName, itemKey, itemConfig);
        }
    }

    private void validateRequiredProperties(String menuName, String itemKey, ConfigurationSection itemConfig) {
        List<String> requiredProps = plugin.getConfig().getStringList("menus.item_validation.required_properties");
        
        for (String prop : requiredProps) {
            if (!itemConfig.contains(prop)) {
                plugin.getLogger().warning("Missing required property '" + prop + "' in menu " + menuName + " item " + itemKey);
                
                if (autoFixIssues) {
                    switch (prop) {
                        case "material":
                            itemConfig.set(prop, "PAPER");
                            break;
                        case "slot":
                            itemConfig.set(prop, 0);
                            break;
                        case "name":
                            itemConfig.set(prop, "&f" + itemKey);
                            break;
                        case "action":
                            itemConfig.set(prop, "close");
                            break;
                    }
                    plugin.debug("Auto-fixed missing property %s for item %s in menu %s", prop, itemKey, menuName);
                }
            }
        }
    }

    private void validateItemMaterial(String menuName, String itemKey, ConfigurationSection itemConfig) {
        String materialName = itemConfig.getString("material");
        if (materialName == null) return;
        
        List<String> validMaterials = plugin.getConfig().getStringList("menus.item_validation.valid_materials");
        
        try {
            Material.valueOf(materialName);
            if (!validMaterials.contains(materialName)) {
                plugin.getLogger().warning("Uncommon material '" + materialName + "' used in menu " + menuName + " item " + itemKey);
            }
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid material '" + materialName + "' in menu " + menuName + " item " + itemKey);
            
            if (autoFixIssues) {
                itemConfig.set("material", "PAPER");
                plugin.debug("Auto-fixed invalid material for item %s in menu %s", itemKey, menuName);
            }
        }
    }

    private void validateItemAction(String menuName, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action");
        if (action == null) return;
        
        List<String> validActions = plugin.getConfig().getStringList("menus.link_validation.valid_actions");
        
        String actionType = action.split(":")[0];
        if (!validActions.contains(actionType)) {
            plugin.getLogger().warning("Invalid action type '" + actionType + "' in menu " + menuName + " item " + itemKey);
            
            if (autoFixIssues) {
                itemConfig.set("action", "close");
                plugin.debug("Auto-fixed invalid action for item %s in menu %s", itemKey, menuName);
            }
            return;
        }
        
        validateActionParameters(menuName, itemKey, action, actionType);
    }

    private void validateActionParameters(String menuName, String itemKey, String action, String actionType) {
        ConfigurationSection actionConfig = plugin.getConfig().getConfigurationSection("menus.link_validation.action_requirements." + actionType);
        if (actionConfig == null) return;
        
        List<String> requiredParams = actionConfig.getStringList("required_params");
        String[] actionParts = action.split(":");
        
        for (String param : requiredParams) {
            boolean paramFound = false;
            
            switch (actionType) {
                case "open_menu":
                    if (param.equals("menu_type") && actionParts.length > 1) {
                        String menuType = actionParts[1];
                        List<String> validMenuTypes = actionConfig.getStringList("valid_menu_types");
                        if (!validMenuTypes.contains(menuType)) {
                            plugin.getLogger().warning("Invalid menu type '" + menuType + "' in menu " + menuName + " item " + itemKey);
                        }
                        paramFound = true;
                    }
                    break;
                    
                case "execute_punishment":
                    if (param.equals("punishment_type") && actionParts.length > 1) {
                        String punishmentType = actionParts[1];
                        List<String> validTypes = actionConfig.getStringList("valid_punishment_types");
                        if (!validTypes.contains(punishmentType)) {
                            plugin.getLogger().warning("Invalid punishment type '" + punishmentType + "' in menu " + menuName + " item " + itemKey);
                        }
                        paramFound = true;
                    }
                    break;
                    
                case "select_duration":
                    if (param.equals("duration") && actionParts.length > 1) {
                        String duration = actionParts[1];
                        List<String> validDurations = actionConfig.getStringList("valid_durations");
                        if (!validDurations.contains(duration)) {
                            plugin.getLogger().warning("Invalid duration '" + duration + "' in menu " + menuName + " item " + itemKey);
                        }
                        paramFound = true;
                    }
                    break;
            }
            
            if (!paramFound) {
                plugin.getLogger().warning("Missing required parameter '" + param + "' for action '" + actionType + "' in menu " + menuName + " item " + itemKey);
            }
        }
    }

    private void validateMenuLinks(String menuName, String itemKey, ConfigurationSection itemConfig) {
        String action = itemConfig.getString("action");
        if (action == null || !action.startsWith("open_menu:")) return;
        
        String[] parts = action.split(":");
        if (parts.length < 2) return;
        
        String targetMenu = parts[1];
        File targetFile = new File(plugin.getDataFolder(), "gui/" + targetMenu + ".yml");
        
        if (!targetFile.exists()) {
            plugin.getLogger().warning("Menu link points to non-existent menu: " + targetMenu + " (from " + menuName + " item " + itemKey + ")");
            
            if (autoFixIssues) {
                itemConfig.set("action", "close");
                plugin.debug("Auto-fixed broken menu link for item %s in menu %s", itemKey, menuName);
            }
        }
    }

    public void validateMenuPerformance() {
        if (!plugin.getConfig().getBoolean("menus.performance.cache_menu_configs", true)) return;
        
        plugin.debug("Validating menu performance settings");
        
        File guiDir = new File(plugin.getDataFolder(), "gui");
        if (!guiDir.exists()) return;
        
        File[] menuFiles = guiDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (menuFiles == null) return;
        
        int totalItems = 0;
        for (File menuFile : menuFiles) {
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(menuFile);
                ConfigurationSection items = config.getConfigurationSection("items");
                if (items != null) {
                    totalItems += items.getKeys(false).size();
                }
            } catch (Exception e) {
                plugin.debug("Error counting items in menu file %s: %s", menuFile.getName(), e.getMessage());
            }
        }
        
        plugin.debug("Menu performance validation: %d menu files, %d total items", menuFiles.length, totalItems);
        
        if (totalItems > 1000) {
            plugin.getLogger().info("Large number of menu items detected (" + totalItems + "). Consider enabling menu caching for better performance.");
        }
    }

    public boolean isValidationEnabled() {
        return validationEnabled;
    }

    public void reloadValidationSettings() {
        plugin.debug("Reloading menu validation settings");
    }
}
