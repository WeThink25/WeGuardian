package me.wethink.weGuardian.gui;

import me.wethink.weGuardian.WeGuardian;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class GUIConfigLoader {

    private final WeGuardian plugin;
    private final Map<String, FileConfiguration> guiConfigs = new HashMap<>();
    private final String[] guiFiles = {"main", "ban", "tempban", "mute", "tempmute", "kick", "warn", "notes", "unban", "unmute"};

    public GUIConfigLoader(WeGuardian plugin) {
        this.plugin = plugin;
        loadAllGUIConfigs();
    }

    private void loadAllGUIConfigs() {
        File guiFolder = new File(plugin.getDataFolder(), "gui");
        if (!guiFolder.exists()) {
            guiFolder.mkdirs();
        }

        for (String guiName : guiFiles) {
            String fileName = guiName + ".yml";
            File guiFile = new File(guiFolder, fileName);

            if (!guiFile.exists()) {
                saveDefaultGUIFile(fileName, guiFile);
            }

            try {
                FileConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
                guiConfigs.put(guiName, config);
                plugin.getLogger().info("Loaded GUI config: " + fileName);
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to load GUI config " + fileName + ": " + e.getMessage());
            }
        }
    }

    private void saveDefaultGUIFile(String fileName, File guiFile) {
        try (InputStream resource = plugin.getResource("gui/" + fileName)) {
            if (resource != null) {
                Files.copy(resource, guiFile.toPath());
                plugin.getLogger().info("Created default GUI file: " + fileName);
            } else {
                plugin.getLogger().warning("Default GUI file not found in resources: " + fileName);
                createMinimalGUIFile(guiFile, fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save default GUI file " + fileName + ": " + e.getMessage());
            createMinimalGUIFile(guiFile, fileName);
        }
    }

    private void createMinimalGUIFile(File guiFile, String fileName) {
        try {
            if (guiFile.createNewFile()) {
                plugin.getLogger().info("Created minimal GUI file: " + fileName);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to create minimal GUI file " + fileName + ": " + e.getMessage());
        }
    }

    public FileConfiguration getGUIConfig(String guiName) {
        return guiConfigs.get(guiName);
    }

    public void reloadGUIConfig(String guiName) {
        File guiFile = new File(plugin.getDataFolder(), "gui/" + guiName + ".yml");
        if (guiFile.exists()) {
            FileConfiguration config = YamlConfiguration.loadConfiguration(guiFile);
            guiConfigs.put(guiName, config);
        }
    }

    public void reloadAllGUIConfigs() {
        guiConfigs.clear();
        loadAllGUIConfigs();
    }
}
