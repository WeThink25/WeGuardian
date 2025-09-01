package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import me.wethink.weGuardian.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ConsoleCommandService {

    private final WeGuardian plugin;

    public ConsoleCommandService(WeGuardian plugin) {
        this.plugin = plugin;
    }

    public void executeConsoleCommands(Punishment punishment) {
        if (!plugin.getConfig().getBoolean("console_commands.enabled", false)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String eventKey = getEventKey(punishment.getType());
            if (eventKey == null) return;

            ConfigurationSection commandsSection = plugin.getConfig().getConfigurationSection("console_commands." + eventKey);
            if (commandsSection == null) {
                List<String> commands = plugin.getConfig().getStringList("console_commands." + eventKey);
                if (commands != null && !commands.isEmpty()) {
                    executeCommands(commands, punishment);
                }
                return;
            }

            List<String> commands = plugin.getConfig().getStringList("console_commands." + eventKey);
            if (commands != null && !commands.isEmpty()) {
                executeCommands(commands, punishment);
            }
        });
    }

    public void executeUnpunishmentCommands(Punishment punishment, String staffName) {
        if (!plugin.getConfig().getBoolean("console_commands.enabled", false)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            String eventKey = getUnpunishmentEventKey(punishment.getType());
            if (eventKey == null) return;

            List<String> commands = plugin.getConfig().getStringList("console_commands." + eventKey);
            if (commands != null && !commands.isEmpty()) {
                executeUnpunishmentCommands(commands, punishment, staffName);
            }
        });
    }

    private void executeCommands(List<String> commands, Punishment punishment) {
        for (String command : commands) {
            String processedCommand = processPlaceholders(command, punishment);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to execute console command: " + processedCommand + " - " + e.getMessage());
                }
            });
        }
    }

    private void executeUnpunishmentCommands(List<String> commands, Punishment punishment, String staffName) {
        for (String command : commands) {
            String processedCommand = processUnpunishmentPlaceholders(command, punishment, staffName);
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to execute console command: " + processedCommand + " - " + e.getMessage());
                }
            });
        }
    }

    private String processPlaceholders(String command, Punishment punishment) {
        String processed = command;
        
        processed = processed.replace("{player}", punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown");
        processed = processed.replace("{staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "Console");
        processed = processed.replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "No reason");
        processed = processed.replace("{type}", punishment.getType().name().toLowerCase());
        processed = processed.replace("{server}", punishment.getServerName() != null ? punishment.getServerName() : "Unknown");
        
        if (punishment.getExpiresAt() != null) {
            processed = processed.replace("{duration}", MessageUtils.formatPunishmentMessage("{duration}", punishment));
            processed = processed.replace("{expires}", MessageUtils.formatPunishmentMessage("{expires}", punishment));
        } else {
            processed = processed.replace("{duration}", "Permanent");
            processed = processed.replace("{expires}", "Never");
        }
        
        return MessageUtils.colorize(processed);
    }

    private String processUnpunishmentPlaceholders(String command, Punishment punishment, String staffName) {
        String processed = command;
        
        processed = processed.replace("{player}", punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown");
        processed = processed.replace("{staff}", staffName != null ? staffName : "Console");
        processed = processed.replace("{original_staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "Console");
        processed = processed.replace("{original_reason}", punishment.getReason() != null ? punishment.getReason() : "No reason");
        processed = processed.replace("{type}", punishment.getType().name().toLowerCase());
        processed = processed.replace("{server}", punishment.getServerName() != null ? punishment.getServerName() : "Unknown");
        
        return MessageUtils.colorize(processed);
    }

    private String getEventKey(PunishmentType type) {
        return switch (type) {
            case BAN, IPBAN -> "on_ban";
            case TEMPBAN -> "on_tempban";
            case MUTE, IPMUTE -> "on_mute";
            case TEMPMUTE -> "on_tempmute";
            case KICK, IPKICK -> "on_kick";
            case WARN, IPWARN -> "on_warn";
            default -> null;
        };
    }

    private String getUnpunishmentEventKey(PunishmentType type) {
        return switch (type) {
            case BAN, TEMPBAN, IPBAN -> "on_unban";
            case MUTE, TEMPMUTE, IPMUTE -> "on_unmute";
            default -> null;
        };
    }
}
