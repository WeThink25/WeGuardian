package me.wethink.weGuardian.managers;

import me.wethink.weGuardian.WeGuardian;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Deprecated
public class GUIManager {

    private final WeGuardian plugin;
    private final Map<String, FileConfiguration> guiConfigs;
    private int loadedGUIsCount = 0;

    public GUIManager(WeGuardian plugin) {
        this.plugin = plugin;
        this.guiConfigs = new HashMap<>();
        plugin.getLogger().warning("GUIManager is deprecated. Use PunishmentGUI and GUIConfigLoader instead.");
    }

    @Deprecated
    public FileConfiguration getGUIConfig(String configName) {
        plugin.getLogger().warning("GUIManager.getGUIConfig() is deprecated. Use WeGuardian.getGuiConfig() instead.");
        return plugin.getGuiConfig();
    }

    @Deprecated
    public void reloadGUIConfigs() {
        plugin.getLogger().warning("GUIManager.reloadGUIConfigs() is deprecated. Use WeGuardian.reloadConfigurations() instead.");
        plugin.reloadConfigurations();
    }

    @Deprecated
    public FileConfiguration getPunishmentMenuConfig() {
        return plugin.getGuiConfig();
    }

    @Deprecated
    public FileConfiguration getReasonsConfig() {
        return plugin.getReasonsConfig();
    }

    @Deprecated
    public String getMenuTitle() {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getString("menu.title", "&c&lPunishment Menu") : "&c&lPunishment Menu";
    }

    @Deprecated
    public int getMenuSize() {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getInt("menu.size", 54) : 54;
    }

    @Deprecated
    public int getMenuUpdateInterval() {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getInt("menu.update_interval", 20) : 20;
    }

    @Deprecated
    public String getItemName(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getString("items." + itemKey + ".name", "&cItem") : "&cItem";
    }

    @Deprecated
    public List<String> getItemLore(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getStringList("items." + itemKey + ".lore") : List.of();
    }

    @Deprecated
    public String getItemMaterial(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getString("items." + itemKey + ".material", "BARRIER") : "BARRIER";
    }

    @Deprecated
    public int getItemSlot(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getInt("items." + itemKey + ".slot", 0) : 0;
    }

    @Deprecated
    public String getItemPermission(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getString("items." + itemKey + ".permission", "") : "";
    }

    @Deprecated
    public boolean isItemEnabled(String itemKey) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getBoolean("items." + itemKey + ".enabled", true) : true;
    }

    @Deprecated
    public List<String> getReasonsByCategory(String category) {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getStringList("categories." + category + ".reasons") : List.of();
    }

    @Deprecated
    public String getCategoryDisplayName(String category) {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getString("categories." + category + ".display_name", category) : category;
    }

    @Deprecated
    public String getCategoryMaterial(String category) {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getString("categories." + category + ".material", "PAPER") : "PAPER";
    }

    @Deprecated
    public boolean isCategoryEnabled(String category) {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getBoolean("categories." + category + ".enabled", true) : true;
    }

    @Deprecated
    public boolean isCustomReasonsAllowed() {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getBoolean("settings.allow_custom_reasons", true) : true;
    }

    @Deprecated
    public int getMaxCustomReasonLength() {
        FileConfiguration config = plugin.getReasonsConfig();
        return config != null ? config.getInt("settings.max_custom_reason_length", 100) : 100;
    }

    @Deprecated
    public List<String> getAvailableCategories() {
        FileConfiguration config = plugin.getReasonsConfig();
        if (config != null && config.getConfigurationSection("categories") != null) {
            return List.copyOf(config.getConfigurationSection("categories").getKeys(false));
        }
        return List.of();
    }

    @Deprecated
    public List<String> getQuickActionDurations(String punishmentType) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getStringList("quick_actions." + punishmentType + ".durations") : List.of();
    }

    @Deprecated
    public String getQuickActionDefaultReason(String punishmentType) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getString("quick_actions." + punishmentType + ".default_reason", "No reason specified") : "No reason specified";
    }

    @Deprecated
    public boolean isQuickActionEnabled(String punishmentType) {
        FileConfiguration config = plugin.getGuiConfig();
        return config != null ? config.getBoolean("quick_actions." + punishmentType + ".enabled", false) : false;
    }

    @Deprecated
    public int getLoadedGUIsCount() {
        return loadedGUIsCount;
    }

    @Deprecated
    public Map<String, FileConfiguration> getAllGUIConfigs() {
        return new HashMap<>(guiConfigs);
    }
}
