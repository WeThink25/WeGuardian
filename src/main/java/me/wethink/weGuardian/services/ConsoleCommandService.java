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
            plugin.debug("Console commands skipped: disabled in config");
            return;
        }

        plugin.debug("Executing console commands for punishment: type=%s, target=%s", 
                punishment.getType(), punishment.getTargetName());

        CompletableFuture.runAsync(() -> {
            String eventKey = getEventKey(punishment.getType());
            if (eventKey == null) {
                plugin.debug("No console commands configured for punishment type: %s", punishment.getType());
                return;
            }

            plugin.debug("Looking for console commands with event key: %s", eventKey);
            ConfigurationSection commandsSection = plugin.getConfig().getConfigurationSection("console_commands." + eventKey);
            if (commandsSection == null) {
                List<String> commands = plugin.getConfig().getStringList("console_commands." + eventKey);
                if (commands != null && !commands.isEmpty()) {
                    plugin.debug("Found %d console commands to execute", commands.size());
                    executeCommands(commands, punishment);
                } else {
                    plugin.debug("No console commands found for event key: %s", eventKey);
                }
                return;
            }

            List<String> commands = plugin.getConfig().getStringList("console_commands." + eventKey);
            if (commands != null && !commands.isEmpty()) {
                plugin.debug("Found %d console commands to execute (fallback)", commands.size());
                executeCommands(commands, punishment);
            } else {
                plugin.debug("No console commands found for event key: %s (fallback)", eventKey);
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
        plugin.debug("Executing %d console commands", commands.size());
        for (String command : commands) {
            String processedCommand = processPlaceholders(command, punishment);
            plugin.debug("Processed command: %s", processedCommand);
            
            plugin.getFoliaLib().getScheduler().runNextTick(task -> {
                try {
                    plugin.debug("Dispatching console command: %s", processedCommand);
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                    plugin.debug("Console command executed successfully: %s", processedCommand);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to execute console command: " + processedCommand + " - " + e.getMessage());
                    plugin.debug("Console command execution failed: %s - %s", processedCommand, e.getMessage());
                }
            });
        }
    }

    private void executeUnpunishmentCommands(List<String> commands, Punishment punishment, String staffName) {
        for (String command : commands) {
            String processedCommand = processUnpunishmentPlaceholders(command, punishment, staffName);
            
            plugin.getFoliaLib().getScheduler().runNextTick(task -> {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), processedCommand);
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to execute console command: " + processedCommand + " - " + e.getMessage());
                }
            });
        }
    }

    public void executeCommand(String command) {
        plugin.getFoliaLib().getScheduler().runNextTick(task -> {
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to execute console command: " + command + " - " + e.getMessage());
            }
        });
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
