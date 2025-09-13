package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.database.DatabaseManager;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentTemplate;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class TemplateService {

    private final WeGuardian plugin;
    private final DatabaseManager database;
    private final Map<String, PunishmentTemplate> templates;
    private FileConfiguration templatesConfig;

    public TemplateService(WeGuardian plugin) {
        this.plugin = plugin;
        this.database = plugin.getDatabaseManager();
        this.templates = new ConcurrentHashMap<>();
        loadTemplates();
    }

    private void loadTemplates() {
        File templatesFile = new File(plugin.getDataFolder(), "templates.yml");
        if (!templatesFile.exists()) {
            plugin.saveResource("templates.yml", false);
        }

        templatesConfig = YamlConfiguration.loadConfiguration(templatesFile);
        templates.clear();

        ConfigurationSection templatesSection = templatesConfig.getConfigurationSection("templates");
        if (templatesSection != null) {
            for (String templateName : templatesSection.getKeys(false)) {
                ConfigurationSection templateSection = templatesSection.getConfigurationSection(templateName);
                if (templateSection != null) {
                    loadTemplate(templateName, templateSection);
                }
            }
        }

        plugin.debug("Loaded " + templates.size() + " punishment templates");
    }

    private void loadTemplate(String name, ConfigurationSection section) {
        try {
            String category = section.getString("category", "general");
            boolean enabled = section.getBoolean("enabled", true);

            List<PunishmentTemplate.EscalationLevel> levels = new ArrayList<>();
            ConfigurationSection levelsSection = section.getConfigurationSection("escalation");

            if (levelsSection != null) {
                for (String levelKey : levelsSection.getKeys(false)) {
                    ConfigurationSection levelSection = levelsSection.getConfigurationSection(levelKey);
                    if (levelSection != null) {
                        int level = Integer.parseInt(levelKey);
                        PunishmentType type = PunishmentType.valueOf(levelSection.getString("type", "WARN").toUpperCase());
                        String duration = levelSection.getString("duration");
                        String reason = levelSection.getString("reason", "Violation of server rules");

                        levels.add(new PunishmentTemplate.EscalationLevel(level, type, duration, reason));
                    }
                }
            }

            levels.sort(Comparator.comparingInt(PunishmentTemplate.EscalationLevel::getLevel));

            PunishmentTemplate template = new PunishmentTemplate(name, category, levels);
            template.setEnabled(enabled);

            templates.put(name, template);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to load template '" + name + "': " + e.getMessage());
        }
    }

    public CompletableFuture<PunishmentTemplate.EscalationLevel> calculatePunishment(UUID playerUuid, String templateName) {
        return CompletableFuture.supplyAsync(() -> {
            PunishmentTemplate template = templates.get(templateName);
            if (template == null || !template.isEnabled()) {
                return null;
            }

            try {
                List<Punishment> history = database.getPunishmentHistory(playerUuid).join();
                int offenseCount = (int) history.stream()
                        .filter(p -> p.getReason().contains(template.getCategory()) ||
                                p.getReason().contains(templateName))
                        .count();

                return template.getEscalationLevel(offenseCount + 1);

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to calculate punishment for template '" + templateName + "': " + e.getMessage());
                return template.getEscalationLevel(1);
            }
        });
    }

    private int getEscalationLevel(UUID playerUuid, String templateName) {
        try {
            PunishmentTemplate template = templates.get(templateName);
            if (template == null) {
                return 1;
            }

            List<Punishment> history = database.getPunishmentHistory(playerUuid).join();
            int offenseCount = (int) history.stream()
                    .filter(p -> p.getReason().contains(template.getCategory()) ||
                            p.getReason().contains(templateName))
                    .count();

            return offenseCount + 1;

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to get escalation level for template '" + templateName + "': " + e.getMessage());
            return 1;
        }
    }

    public CompletableFuture<Boolean> executePunishmentFromTemplate(UUID playerUuid, String playerName,
                                                                    String staffName, String templateName,
                                                                    Map<String, String> context) {
        return CompletableFuture.supplyAsync(() -> {
            if (!templates.containsKey(templateName)) {
                plugin.getLogger().warning("Template '" + templateName + "' not found");
                return false;
            }

            PunishmentTemplate template = templates.get(templateName);

            int escalationLevel = getEscalationLevel(playerUuid, templateName);
            PunishmentTemplate.EscalationLevel level = template.getEscalationLevel(escalationLevel);

            if (level == null) {
                plugin.getLogger().warning("No escalation level found for template '" + templateName + "' at level " + escalationLevel);
                return false;
            }

            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", playerName);
            placeholders.put("staff", staffName);
            if (context != null) {
                placeholders.putAll(context);
            }

            String processedReason = template.processReason(level.getReason(), placeholders);

            PunishmentService punishmentService = plugin.getPunishmentService();

            try {
                return switch (level.getType()) {
                    case BAN -> punishmentService.ban(playerName, staffName, processedReason).get();
                    case TEMPBAN ->
                            punishmentService.tempban(playerName, staffName, processedReason, level.getDuration()).get();
                    case MUTE -> punishmentService.mute(playerName, staffName, processedReason).get();
                    case TEMPMUTE ->
                            punishmentService.tempmute(playerName, staffName, processedReason, level.getDuration()).get();
                    case KICK -> punishmentService.kick(playerName, staffName, processedReason).get();
                    case WARN -> punishmentService.warn(playerName, staffName, processedReason).get();
                    default -> false;
                };
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing template punishment: " + e.getMessage());
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> executePunishmentFromTemplate(UUID playerUuid, String playerName,
                                                                    String staffName, String templateName,
                                                                    String reason) {
        Map<String, String> context = new HashMap<>();
        if (reason != null) {
            context.put("reason", reason);
        }
        return executePunishmentFromTemplate(playerUuid, playerName, staffName, templateName, context);
    }

    public CompletableFuture<Boolean> executeTemplate(org.bukkit.command.CommandSender sender, String targetName, String templateName, String duration) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID targetUuid = plugin.getPunishmentService().getPlayerUUID(targetName).join();
                if (targetUuid == null) {
                    sender.sendMessage("§cPlayer '" + targetName + "' not found.");
                    return false;
                }

                String staffName = sender instanceof org.bukkit.entity.Player ? sender.getName() : "Console";
                
                Map<String, String> context = new HashMap<>();
                if (duration != null && !duration.isEmpty()) {
                    context.put("duration", duration);
                }
                
                return executePunishmentFromTemplate(targetUuid, targetName, staffName, templateName, context).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing template: " + e.getMessage());
                sender.sendMessage("§cError executing template: " + e.getMessage());
                return false;
            }
        });
    }

    public PunishmentTemplate getTemplate(String name) {
        return templates.get(name);
    }

    public Collection<PunishmentTemplate> getAllTemplates() {
        return templates.values();
    }

    public Collection<PunishmentTemplate> getTemplatesByCategory(String category) {
        return templates.values().stream()
                .filter(template -> template.getCategory().equalsIgnoreCase(category))
                .toList();
    }

    public void reloadTemplates() {
        loadTemplates();
    }

    public CompletableFuture<Boolean> executeIPTemplate(String targetIP, String staffName, String templateName, Map<String, String> context) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                PunishmentTemplate template = getTemplate(templateName);
                if (template == null) {
                    plugin.getLogger().warning("Template '" + templateName + "' not found");
                    return false;
                }

                PunishmentTemplate.EscalationLevel escalationLevel = template.getEscalationLevel(1);
                if (escalationLevel == null) {
                    plugin.getLogger().warning("No escalation levels found for template '" + templateName + "'");
                    return false;
                }

                return plugin.getPunishmentService().ipmute(targetIP, staffName, escalationLevel.getReason()).join();
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing IP template: " + e.getMessage());
                return false;
            }
        });
    }

    public boolean hasTemplate(String name) {
        return templates.containsKey(name);
    }

    public void shutdown() {
        templates.clear();
        plugin.getLogger().info("TemplateService shutdown completed");
    }
}
