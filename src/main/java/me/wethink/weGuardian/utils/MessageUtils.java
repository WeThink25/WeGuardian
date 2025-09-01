package me.wethink.weGuardian.utils;

import me.wethink.weGuardian.models.Punishment;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class MessageUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacyAmpersand();

    public static String colorize(String message) {
        if (message == null) return "";
        return message.replace('&', '§');
    }

    public static Component toComponent(String message) {
        if (message == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(message);
    }

    public static String replacePlaceholders(String message, Map<String, String> placeholders) {
        if (message == null || placeholders == null) return message;

        String result = message;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return result;
    }

    public static Map<String, String> createPunishmentPlaceholders(Punishment punishment) {
        Map<String, String> placeholders = new HashMap<>();

        placeholders.put("player", punishment.getTargetName());
        placeholders.put("staff", punishment.getStaffName());
        placeholders.put("reason", punishment.getReason());
        placeholders.put("type", punishment.getType().getDisplayName());
        placeholders.put("server", punishment.getServerName());
        placeholders.put("date", punishment.getCreatedAt().format(DATE_FORMATTER));
        placeholders.put("datetime", punishment.getCreatedAt().format(TIME_FORMATTER));

        if (punishment.getExpiresAt() != null) {
            placeholders.put("expires", punishment.getExpiresAt().format(TIME_FORMATTER));
            placeholders.put("time", TimeUtils.getRemainingTime(punishment.getExpiresAt()));
        } else {
            placeholders.put("expires", "Never");
            placeholders.put("time", "Permanent");
        }

        return placeholders;
    }

    public static Map<String, String> createPlayerPlaceholders(Player player) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        return placeholders;
    }

    public static Map<String, String> createPlaceholders(String... keyValuePairs) {
        Map<String, String> placeholders = new HashMap<>();

        for (int i = 0; i < keyValuePairs.length - 1; i += 2) {
            placeholders.put(keyValuePairs[i], keyValuePairs[i + 1]);
        }

        return placeholders;
    }

    public static String formatMessage(String template, Map<String, String> placeholders) {
        String message = replacePlaceholders(template, placeholders);
        return colorize(message);
    }

    public static String formatPunishmentMessage(String template, Punishment punishment) {
        if (template == null || punishment == null) return "";

        String message = template;

        message = message.replace("{player}", punishment.getTargetName() != null ? punishment.getTargetName() : "Unknown");
        message = message.replace("{staff}", punishment.getStaffName() != null ? punishment.getStaffName() : "Console");
        message = message.replace("{reason}", punishment.getReason() != null ? punishment.getReason() : "No reason specified");
        
        LocalDateTime createdAt = punishment.getCreatedAt();
        if (createdAt != null) {
            message = message.replace("{date}", createdAt.format(DATE_FORMATTER));
            message = message.replace("{datetime}", createdAt.format(TIME_FORMATTER));
        } else {
            message = message.replace("{date}", "Unknown");
            message = message.replace("{datetime}", "Unknown");
        }

        if (punishment.getExpiresAt() != null) {
            LocalDateTime expiresAt = punishment.getExpiresAt();
            message = message.replace("{expires}", expiresAt.format(TIME_FORMATTER));
            
            LocalDateTime now = LocalDateTime.now();
            if (expiresAt.isAfter(now)) {
                String timeLeft = TimeUtils.getRemainingTime(expiresAt);
                message = message.replace("{time-left}", timeLeft != null ? timeLeft : "Unknown");
            } else {
                message = message.replace("{time-left}", "Expired");
            }

            if (createdAt != null) {
                Duration duration = Duration.between(createdAt, expiresAt);
                long seconds = duration.getSeconds();
                message = message.replace("{duration}", TimeUtils.formatDuration(seconds));
            } else {
                message = message.replace("{duration}", "Unknown");
            }
        } else {
            message = message.replace("{expires}", "Never");
            message = message.replace("{time-left}", "Permanent");
            message = message.replace("{duration}", "Permanent");
        }

        return colorize(message);
    }

    public static String formatPunishmentMessage(String template, Punishment punishment, String appealUrl) {
        String message = formatPunishmentMessage(template, punishment);
        
        if (message.contains("{appeal-url}")) {
            message = message.replace("{appeal-url}", appealUrl);
        }
        
        return message;
    }

    public static String stripColor(String message) {
        if (message == null) return "";
        return message.replaceAll("§[0-9a-fk-or]", "").replaceAll("&[0-9a-fk-or]", "");
    }

    public static String stripColors(String message) {
        if (message == null) return "";
        return LEGACY_SERIALIZER.serialize(LEGACY_SERIALIZER.deserialize(message).decoration(TextDecoration.ITALIC, false));
    }

    public static String centerText(String text, int width) {
        if (text == null || text.length() >= width) return text;

        int padding = (width - stripColors(text).length()) / 2;
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < padding; i++) {
            result.append(" ");
        }
        result.append(text);

        return result.toString();
    }

    public static String createSeparator(char character, int length) {
        StringBuilder separator = new StringBuilder();
        for (int i = 0; i < length; i++) {
            separator.append(character);
        }
        return separator.toString();
    }

    public static String formatList(String... items) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            result.append("&7• &f").append(items[i]);
            if (i < items.length - 1) {
                result.append("\n");
            }
        }
        return result.toString();
    }

    public static TextColor parseColor(String message) {
        if (message == null || message.length() < 2) return TextColor.color(0xFFFFFF);
        
        String colorCode = message.replace('&', '§');
        if (colorCode.length() >= 2) {
            char code = colorCode.charAt(1);
            switch (code) {
                case '0': return TextColor.color(0x000000);
                case '1': return TextColor.color(0x0000AA);
                case '2': return TextColor.color(0x00AA00);
                case '3': return TextColor.color(0x00AAAA);
                case '4': return TextColor.color(0xAA0000);
                case '5': return TextColor.color(0xAA00AA);
                case '6': return TextColor.color(0xFFAA00);
                case '7': return TextColor.color(0xAAAAAA);
                case '8': return TextColor.color(0x555555);
                case '9': return TextColor.color(0x5555FF);
                case 'a': return TextColor.color(0x55FF55);
                case 'b': return TextColor.color(0x55FFFF);
                case 'c': return TextColor.color(0xFF5555);
                case 'd': return TextColor.color(0xFF55FF);
                case 'e': return TextColor.color(0xFFFF55);
                case 'f': return TextColor.color(0xFFFFFF);
                default: return TextColor.color(0xFFFFFF);
            }
        }
        return TextColor.color(0xFFFFFF);
    }
}
