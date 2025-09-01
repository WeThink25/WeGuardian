package me.wethink.weGuardian.managers;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.logging.Level;

public class MessageManager {

    private final WeGuardian plugin;
    private FileConfiguration messagesConfig;
    private int loadedMessagesCount = 0;

    public MessageManager(WeGuardian plugin) {
        this.plugin = plugin;
        loadMessagesConfig();
    }

    private void loadMessagesConfig() {
        try {
            File messagesFile = new File(plugin.getDataFolder(), "messages.yml");

            if (!messagesFile.exists()) {
                plugin.saveResource("messages.yml", false);
                plugin.getLogger().info("Created default messages.yml file");
            }

            messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
            countLoadedMessages();

            plugin.getLogger().info("Loaded " + loadedMessagesCount + " messages from messages.yml");

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load messages.yml", e);
            messagesConfig = new YamlConfiguration();
        }
    }

    private void countLoadedMessages() {
        loadedMessagesCount = 0;
        if (messagesConfig != null) {
            countSection("screen");
            countSection("actionbar");
            countSection("chat");
            countSection("defaults");
            countSection("commands");
            countSection("errors");
        }
    }

    private void countSection(String section) {
        if (messagesConfig.getConfigurationSection(section) != null) {
            loadedMessagesCount += messagesConfig.getConfigurationSection(section).getKeys(true).size();
        }
    }

    public void reloadMessages() {
        loadMessagesConfig();
        plugin.getLogger().info("Messages configuration reloaded");
    }

    public String getMessage(String path) {
        return getMessage(path, "");
    }

    public String getMessage(String path, String defaultMessage) {
        if (messagesConfig == null) {
            return defaultMessage;
        }
        return messagesConfig.getString(path, defaultMessage);
    }

    public List<String> getMessageList(String path) {
        if (messagesConfig == null) {
            return List.of();
        }
        return messagesConfig.getStringList(path);
    }

    public String getKickMessage(Punishment punishment) {
        String messageKey = "screen." + punishment.getType().name().toLowerCase();
        String template = getMessage(messageKey, getDefaultKickMessage(punishment.getType()));
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    public String getActionBarMessage(Punishment punishment) {
        String messageKey = "actionbar." + punishment.getType().name().toLowerCase();
        String template = getMessage(messageKey, getDefaultActionBarMessage(punishment.getType()));
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    public String getChatBroadcastMessage(Punishment punishment) {
        String messageKey = "chat." + punishment.getType().name().toLowerCase();
        String template = getMessage(messageKey, getDefaultChatMessage(punishment.getType()));
        return MessageUtils.formatPunishmentMessage(template, punishment);
    }

    public String getUnpunishmentMessage(Punishment punishment, String staffName) {
        String messageKey = "chat.un" + punishment.getType().name().toLowerCase();
        String template = getMessage(messageKey, getDefaultUnpunishmentMessage(punishment.getType()));
        return template.replace("{player}", punishment.getTargetName())
                .replace("{staff}", staffName);
    }

    public String getCommandMessage(String key) {
        return getMessage("commands." + key, "&cMessage not found: " + key);
    }

    public String getErrorMessage(String key) {
        return getMessage("errors." + key, "&cError message not found: " + key);
    }

    public String getUsageMessage(String command) {
        return getMessage("commands.usage." + command, "&cUsage not found for command: " + command);
    }

    private String getDefaultKickMessage(me.wethink.weGuardian.models.PunishmentType type) {
        return switch (type) {
            case BAN -> "&c&l⚠ YOU ARE BANNED ⚠\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Date: &f{date}";
            case TEMPBAN -> "&e&l⏰ TEMPORARY BAN ⏰\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}\n&7Expires: &f{expires}";
            case KICK -> "&c&l⚡ KICKED ⚡\n\n&7Reason: &f{reason}\n&7Staff: &f{staff}";
            case WARN -> "&e⚠ &6You have been warned!\n&7Reason: &f{reason}\n&7Staff: &f{staff}";
            default -> "&cYou have been punished: {reason}";
        };
    }

    private String getDefaultActionBarMessage(me.wethink.weGuardian.models.PunishmentType type) {
        return switch (type) {
            case MUTE -> "&6🔇 You are muted: &f{reason}";
            case TEMPMUTE -> "&6🔇 Muted: &f{reason} &7| &fExpires: {time-left}";
            case BAN -> "&c⚠ You are banned: &f{reason}";
            case TEMPBAN -> "&e⏰ Temporarily banned: &f{reason} &7| &fExpires: {time-left}";
            default -> "&7You are punished: {reason}";
        };
    }

    private String getDefaultChatMessage(me.wethink.weGuardian.models.PunishmentType type) {
        return switch (type) {
            case BAN -> "&c⚠ &f{player} &chas been banned by &f{staff} &cfor: &f{reason}";
            case TEMPBAN ->
                    "&e⏰ &f{player} &ehas been temporarily banned by &f{staff} &efor: &f{reason} &7({duration})";
            case MUTE -> "&6🔇 &f{player} &6has been muted by &f{staff} &6for: &f{reason}";
            case TEMPMUTE ->
                    "&6🔇 &f{player} &6has been temporarily muted by &f{staff} &6for: &f{reason} &7({duration})";
            case KICK -> "&c⚡ &f{player} &chas been kicked by &f{staff} &cfor: &f{reason}";
            case WARN -> "&e⚠ &f{player} &ehas been warned by &f{staff} &efor: &f{reason}";
            default -> "&7{player} was punished by {staff} for: {reason}";
        };
    }

    private String getDefaultUnpunishmentMessage(me.wethink.weGuardian.models.PunishmentType type) {
        return switch (type) {
            case BAN, TEMPBAN -> "&a✓ &f{player} &ahas been unbanned by &f{staff}";
            case MUTE, TEMPMUTE -> "&a✓ &f{player} &ahas been unmuted by &f{staff}";
            default -> "&a✓ &f{player} &ahas been unpunished by &f{staff}";
        };
    }

    public int getLoadedMessagesCount() {
        return loadedMessagesCount;
    }

    public FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }
}
