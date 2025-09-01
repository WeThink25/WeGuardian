package me.wethink.weGuardian.services;

import me.wethink.weGuardian.WeGuardian;
import me.wethink.weGuardian.models.Punishment;
import me.wethink.weGuardian.models.PunishmentType;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

public class DiscordWebhookService {

    private final WeGuardian plugin;
    private final HttpClient httpClient;
    private static final DateTimeFormatter DISCORD_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public DiscordWebhookService(WeGuardian plugin) {
        this.plugin = plugin;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public void sendPunishmentWebhook(Punishment punishment) {
        FileConfiguration config = plugin.getConfig();
        
        if (!config.getBoolean("discord.enabled", false)) {
            return;
        }

        String eventKey = getEventKey(punishment.getType());
        if (eventKey == null || !config.getBoolean("discord.send_on." + eventKey, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String webhookUrl = config.getString("discord.webhook_url");
                if (webhookUrl == null || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) {
                    return;
                }

                String payload = createPunishmentPayload(punishment, config);
                sendWebhook(webhookUrl, payload);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    public void sendUnpunishmentWebhook(Punishment punishment, String staffName) {
        FileConfiguration config = plugin.getConfig();
        
        if (!config.getBoolean("discord.enabled", false)) {
            return;
        }

        String eventKey = getUnpunishmentEventKey(punishment.getType());
        if (eventKey == null || !config.getBoolean("discord.send_on." + eventKey, true)) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String webhookUrl = config.getString("discord.webhook_url");
                if (webhookUrl == null || webhookUrl.equals("https://discord.com/api/webhooks/YOUR_WEBHOOK_URL")) {
                    return;
                }

                String payload = createUnpunishmentPayload(punishment, staffName, config);
                sendWebhook(webhookUrl, payload);
                
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());
            }
        });
    }

    private void sendWebhook(String webhookUrl, String payload) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            plugin.getLogger().warning("Discord webhook failed with status: " + response.statusCode() + " - " + response.body());
        }
    }

    private String createPunishmentPayload(Punishment punishment, FileConfiguration config) {
        String username = config.getString("discord.username", "WeGuardian");
        String avatarUrl = config.getString("discord.avatar_url", "");
        int embedColor = config.getInt("discord.embed_color", 0xFF0000);

        String title = getTitle(punishment.getType(), false);
        String description = createDescription(punishment);
        String timestamp = punishment.getCreatedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "Z";

        return String.format("""
            {
              "username": "%s",
              "avatar_url": "%s",
              "embeds": [
                {
                  "title": "%s",
                  "description": "%s",
                  "color": %d,
                  "timestamp": "%s",
                  "fields": [
                    {
                      "name": "Player",
                      "value": "%s",
                      "inline": true
                    },
                    {
                      "name": "Staff",
                      "value": "%s",
                      "inline": true
                    },
                    {
                      "name": "Reason",
                      "value": "%s",
                      "inline": false
                    }%s
                  ]
                }
              ]
            }""",
            username,
            avatarUrl,
            title,
            description,
            embedColor,
            timestamp,
            punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown",
            punishment.getStaffName() != null ? punishment.getStaffName() : "Console",
            punishment.getReason() != null ? punishment.getReason() : "No reason specified",
            punishment.getExpiresAt() != null ? String.format("""
                ,
                    {
                      "name": "Duration",
                      "value": "%s",
                      "inline": true
                    },
                    {
                      "name": "Expires",
                      "value": "%s",
                      "inline": true
                    }""",
                    getDurationString(punishment),
                    punishment.getExpiresAt().format(DISCORD_TIME_FORMAT)) : ""
        );
    }

    private String createUnpunishmentPayload(Punishment punishment, String staffName, FileConfiguration config) {
        String username = config.getString("discord.username", "WeGuardian");
        String avatarUrl = config.getString("discord.avatar_url", "");
        int embedColor = 0x00FF00;

        String title = getTitle(punishment.getType(), true);
        String description = String.format("%s has been %s", 
            punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown",
            punishment.getType() == PunishmentType.BAN || punishment.getType() == PunishmentType.TEMPBAN ? "unbanned" : "unmuted");

        return String.format("""
            {
              "username": "%s",
              "avatar_url": "%s",
              "embeds": [
                {
                  "title": "%s",
                  "description": "%s",
                  "color": %d,
                  "fields": [
                    {
                      "name": "Player",
                      "value": "%s",
                      "inline": true
                    },
                    {
                      "name": "Staff",
                      "value": "%s",
                      "inline": true
                    },
                    {
                      "name": "Original Reason",
                      "value": "%s",
                      "inline": false
                    }
                  ]
                }
              ]
            }""",
            username,
            avatarUrl,
            title,
            description,
            embedColor,
            punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown",
            staffName != null ? staffName : "Console",
            punishment.getReason() != null ? punishment.getReason() : "No reason specified"
        );
    }

    private String createDescription(Punishment punishment) {
        String action = switch (punishment.getType()) {
            case BAN, IPBAN -> "banned";
            case TEMPBAN -> "temporarily banned";
            case MUTE, IPMUTE -> "muted";
            case TEMPMUTE -> "temporarily muted";
            case KICK, IPKICK -> "kicked";
            case WARN, IPWARN -> "warned";
            default -> "punished";
        };

        return String.format("%s has been %s", 
            punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown", action);
    }

    private String getTitle(PunishmentType type, boolean isUnpunishment) {
        if (isUnpunishment) {
            return switch (type) {
                case BAN, TEMPBAN, IPBAN -> "Player Unbanned";
                case MUTE, TEMPMUTE, IPMUTE -> "Player Unmuted";
                default -> "Punishment Removed";
            };
        }

        return switch (type) {
            case BAN, IPBAN -> "Player Banned";
            case TEMPBAN -> "Player Temporarily Banned";
            case MUTE, IPMUTE -> "Player Muted";
            case TEMPMUTE -> "Player Temporarily Muted";
            case KICK, IPKICK -> "Player Kicked";
            case WARN, IPWARN -> "Player Warned";
            default -> "Player Punished";
        };
    }

    private String getDurationString(Punishment punishment) {
        if (punishment.getExpiresAt() == null) {
            return "Permanent";
        }

        java.time.Duration duration = java.time.Duration.between(punishment.getCreatedAt(), punishment.getExpiresAt());
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours, minutes);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes);
        } else {
            return String.format("%dm", minutes);
        }
    }

    private String getEventKey(PunishmentType type) {
        return switch (type) {
            case BAN, IPBAN -> "ban";
            case TEMPBAN -> "tempban";
            case MUTE, IPMUTE -> "mute";
            case TEMPMUTE -> "tempmute";
            case KICK, IPKICK -> "kick";
            case WARN, IPWARN -> "warn";
            default -> null;
        };
    }

    private String getUnpunishmentEventKey(PunishmentType type) {
        return switch (type) {
            case BAN, TEMPBAN, IPBAN -> "unban";
            case MUTE, TEMPMUTE, IPMUTE -> "unmute";
            default -> null;
        };
    }
}
