package me.wethink.weguardian.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class MessagesManager {

    private final JavaPlugin plugin;
    private FileConfiguration messagesConfig;
    private File messagesFile;

    public MessagesManager(JavaPlugin plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");

        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }

        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messagesConfig.setDefaults(defaultConfig);
        }
    }

    public void reload() {
        loadMessages();
    }

    public String getMessage(String path) {
        return messagesConfig.getString(path, "");
    }

    public String getMessage(String path, String defaultValue) {
        return messagesConfig.getString(path, defaultValue);
    }

    public String getMessage(String path, String... replacements) {
        String message = getMessage(path);
        if (message == null || replacements.length == 0) {
            return message;
        }
        for (int i = 0; i < replacements.length - 1; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }

    public List<String> getMessageList(String path) {
        return messagesConfig.getStringList(path);
    }

    public List<String> getMessageList(String path, String... replacements) {
        List<String> messages = getMessageList(path);
        if (messages.isEmpty() || replacements.length == 0) {
            return messages;
        }
        return messages.stream()
                .map(msg -> {
                    String result = msg;
                    for (int i = 0; i < replacements.length - 1; i += 2) {
                        result = result.replace(replacements[i], replacements[i + 1]);
                    }
                    return result;
                })
                .toList();
    }

    public String getPrefix() {
        return getMessage("prefix", "&8[&c&lWeGuardian&8]&r ");
    }

    public FileConfiguration getConfig() {
        return messagesConfig;
    }
}
