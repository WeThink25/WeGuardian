package me.wethink.weguardian.webhook;

import me.wethink.weguardian.WeGuardian;
import me.wethink.weguardian.model.Punishment;
import me.wethink.weguardian.util.TimeUtil;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;


public class DiscordWebhookManager {

    private static final int MAX_EMBEDS_PER_MESSAGE = 10;
    private static final int BATCH_INTERVAL_SECONDS = 5;

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT.withZone(ZoneId.of("UTC"));

    private static final int COLOR_BAN = 0xFF0000;
    private static final int COLOR_TEMPBAN = 0xFF6600;
    private static final int COLOR_MUTE = 0xFFFF00;
    private static final int COLOR_TEMPMUTE = 0x00FF00;
    private static final int COLOR_KICK = 0x0099FF;
    private static final int COLOR_UNBAN = 0x00FF88;
    private static final int COLOR_UNMUTE = 0x88FF00;
    private static final int COLOR_IP_BAN = 0xCC0000;
    private static final int COLOR_IP_MUTE = 0xCCCC00;

    private final WeGuardian plugin;
    private final ConcurrentLinkedQueue<WebhookEmbed> pendingEmbeds;
    private String webhookUrl;
    private boolean enabled;

    public DiscordWebhookManager(WeGuardian plugin) {
        this.plugin = plugin;
        this.pendingEmbeds = new ConcurrentLinkedQueue<>();
        reload();
        startBatchTask();
    }


    public void reload() {
        this.enabled = plugin.getConfig().getBoolean("discord.enabled", false);
        this.webhookUrl = plugin.getConfig().getString("discord.webhook-url", "");

        if (enabled && (webhookUrl == null || webhookUrl.isEmpty() || webhookUrl.equals("YOUR_WEBHOOK_URL_HERE"))) {
            plugin.getLogger().warning("Discord webhook is enabled but no valid URL is configured!");
            this.enabled = false;
        }

        if (enabled) {
            plugin.getLogger().info("Discord webhook logging enabled.");
        }
    }


    private void startBatchTask() {
        plugin.getSchedulerManager().runAsyncRepeating(
                this::sendBatch,
                BATCH_INTERVAL_SECONDS,
                BATCH_INTERVAL_SECONDS,
                TimeUnit.SECONDS);
    }


    public void logPunishment(Punishment punishment) {
        if (!enabled)
            return;

        WebhookEmbed embed = createPunishmentEmbed(punishment);
        pendingEmbeds.offer(embed);
    }


    public void logUnban(String targetName, String staffName) {
        if (!enabled)
            return;

        WebhookEmbed embed = new WebhookEmbed(
                "🔓 Player Unbanned",
                String.format("**%s** has been unbanned by **%s**", targetName, staffName),
                COLOR_UNBAN,
                Instant.now());
        pendingEmbeds.offer(embed);
    }


    public void logUnmute(String targetName, String staffName) {
        if (!enabled)
            return;

        WebhookEmbed embed = new WebhookEmbed(
                "🔊 Player Unmuted",
                String.format("**%s** has been unmuted by **%s**", targetName, staffName),
                COLOR_UNMUTE,
                Instant.now());
        pendingEmbeds.offer(embed);
    }


    public void logUnbanIp(String ipAddress, String targetName, String staffName) {
        if (!enabled)
            return;

        WebhookEmbed embed = new WebhookEmbed(
                "🔓 IP Unbanned",
                String.format("IP `%s` (**%s**) has been unbanned by **%s**", ipAddress, targetName, staffName),
                COLOR_UNBAN,
                Instant.now());
        pendingEmbeds.offer(embed);
    }


    public void logUnmuteIp(String ipAddress, String targetName, String staffName) {
        if (!enabled)
            return;

        WebhookEmbed embed = new WebhookEmbed(
                "🔊 IP Unmuted",
                String.format("IP `%s` (**%s**) has been unmuted by **%s**", ipAddress, targetName, staffName),
                COLOR_UNMUTE,
                Instant.now());
        pendingEmbeds.offer(embed);
    }


    private WebhookEmbed createPunishmentEmbed(Punishment punishment) {
        String title = getEmojiForType(punishment.getType()) + " " + punishment.getType().getDisplayName();

        StringBuilder description = new StringBuilder();
        description.append("**Player:** ").append(punishment.getTargetName()).append("\n");
        description.append("**Staff:** ").append(punishment.getStaffName()).append("\n");
        description.append("**Reason:** ").append(
                punishment.getReason() != null ? punishment.getReason() : "No reason specified").append("\n");

        if (punishment.getTargetIp() != null && !punishment.getTargetIp().isEmpty()) {
            description.append("**IP:** `").append(punishment.getTargetIp()).append("`\n");
        }

        if (punishment.getType().isTemporary() && punishment.getExpiresAt() != null) {
            description.append("**Duration:** ").append(TimeUtil.formatRemaining(punishment.getExpiresAt()))
                    .append("\n");
            description.append("**Expires:** <t:").append(punishment.getExpiresAt().getEpochSecond()).append(":R>");
        } else if (punishment.getType() != me.wethink.weguardian.model.PunishmentType.KICK) {
            description.append("**Duration:** Permanent");
        }

        return new WebhookEmbed(
                title,
                description.toString(),
                getColorForType(punishment.getType()),
                punishment.getCreatedAt());
    }


    private String getEmojiForType(me.wethink.weguardian.model.PunishmentType type) {
        return switch (type) {
            case BAN -> "🔨";
            case TEMPBAN -> "⏰";
            case BANIP -> "🌐";
            case TEMPBANIP -> "🌍";
            case MUTE -> "🔇";
            case TEMPMUTE -> "⏱️";
            case MUTEIP -> "📵";
            case TEMPMUTEIP -> "📴";
            case KICK -> "👢";
        };
    }


    private int getColorForType(me.wethink.weguardian.model.PunishmentType type) {
        return switch (type) {
            case BAN -> COLOR_BAN;
            case TEMPBAN -> COLOR_TEMPBAN;
            case BANIP, TEMPBANIP -> COLOR_IP_BAN;
            case MUTE -> COLOR_MUTE;
            case TEMPMUTE -> COLOR_TEMPMUTE;
            case MUTEIP, TEMPMUTEIP -> COLOR_IP_MUTE;
            case KICK -> COLOR_KICK;
        };
    }


    private void sendBatch() {
        if (!enabled || pendingEmbeds.isEmpty())
            return;

        List<WebhookEmbed> batch = new ArrayList<>(MAX_EMBEDS_PER_MESSAGE);
        WebhookEmbed embed;
        while ((embed = pendingEmbeds.poll()) != null && batch.size() < MAX_EMBEDS_PER_MESSAGE) {
            batch.add(embed);
        }

        if (batch.isEmpty())
            return;

        String json = buildJsonPayload(batch);

        try {
            sendWebhook(json);
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to send Discord webhook: " + e.getMessage());

        }
    }


    private String buildJsonPayload(List<WebhookEmbed> embeds) {
        StringBuilder json = new StringBuilder();
        json.append("{\"embeds\":[");

        for (int i = 0; i < embeds.size(); i++) {
            WebhookEmbed embed = embeds.get(i);
            if (i > 0)
                json.append(",");

            json.append("{");
            json.append("\"title\":\"").append(escapeJson(embed.title)).append("\",");
            json.append("\"description\":\"").append(escapeJson(embed.description)).append("\",");
            json.append("\"color\":").append(embed.color).append(",");
            json.append("\"timestamp\":\"").append(ISO_FORMATTER.format(embed.timestamp)).append("\"");
            json.append("}");
        }

        json.append("]}");
        return json.toString();
    }


    private String escapeJson(String text) {
        if (text == null)
            return "";
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }


    private void sendWebhook(String json) throws IOException {
        URL url = URI.create(webhookUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "WeGuardian/" + plugin.getPluginMeta().getVersion());
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(json.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                plugin.getLogger().warning("Discord webhook rate limited. Consider increasing batch interval.");
            } else if (responseCode < 200 || responseCode >= 300) {
                plugin.getLogger().warning("Discord webhook returned code: " + responseCode);
            }
        } finally {
            connection.disconnect();
        }
    }


    private record WebhookEmbed(String title, String description, int color, Instant timestamp) {
    }
}
