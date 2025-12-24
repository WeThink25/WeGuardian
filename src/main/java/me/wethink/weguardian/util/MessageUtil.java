package me.wethink.weguardian.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageUtil {

    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final LegacyComponentSerializer AMPERSAND = LegacyComponentSerializer.legacyAmpersand();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();
    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern SECTION_PATTERN = Pattern.compile("§[0-9a-fk-orA-FK-OR]");

    private MessageUtil() {
    }

    public static String colorize(String message) {
        if (message == null || message.isEmpty()) {
            return message;
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (matcher.find()) {
            String hex = matcher.group(1);
            StringBuilder replacement = new StringBuilder("§x");
            for (char c : hex.toCharArray()) {
                replacement.append('§').append(c);
            }
            matcher.appendReplacement(buffer, replacement.toString());
        }
        matcher.appendTail(buffer);

        return translateAmpersandCodes(buffer.toString());
    }

    private static String translateAmpersandCodes(String message) {
        char[] chars = message.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == '&' && isColorCode(chars[i + 1])) {
                chars[i] = '§';
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }
        return new String(chars);
    }

    private static boolean isColorCode(char c) {
        return "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(c) > -1;
    }

    private static final Pattern MINI_MESSAGE_PATTERN = Pattern.compile("<[a-zA-Z_#]+[^>]*>");

    public static Component toComponent(String message) {
        if (message == null || message.isEmpty()) {
            return Component.empty();
        }

        boolean hasMiniMessage = MINI_MESSAGE_PATTERN.matcher(message).find();
        boolean hasLegacy = message.contains("&") || message.contains("§");

        if (hasMiniMessage && !hasLegacy) {
            try {
                return MINI_MESSAGE.deserialize(message);
            } catch (Exception e) {
                return Component.text(message);
            }
        }

        if (hasLegacy && !hasMiniMessage) {
            return LEGACY.deserialize(colorize(message));
        }

        if (hasMiniMessage && hasLegacy) {
            try {
                String legacyToMini = convertLegacyToMiniMessage(message);
                return MINI_MESSAGE.deserialize(legacyToMini);
            } catch (Exception e) {
                return LEGACY.deserialize(colorize(message));
            }
        }

        return Component.text(message);
    }

    private static String convertLegacyToMiniMessage(String message) {
        message = message.replace("&0", "<black>").replace("§0", "<black>");
        message = message.replace("&1", "<dark_blue>").replace("§1", "<dark_blue>");
        message = message.replace("&2", "<dark_green>").replace("§2", "<dark_green>");
        message = message.replace("&3", "<dark_aqua>").replace("§3", "<dark_aqua>");
        message = message.replace("&4", "<dark_red>").replace("§4", "<dark_red>");
        message = message.replace("&5", "<dark_purple>").replace("§5", "<dark_purple>");
        message = message.replace("&6", "<gold>").replace("§6", "<gold>");
        message = message.replace("&7", "<gray>").replace("§7", "<gray>");
        message = message.replace("&8", "<dark_gray>").replace("§8", "<dark_gray>");
        message = message.replace("&9", "<blue>").replace("§9", "<blue>");
        message = message.replace("&a", "<green>").replace("§a", "<green>");
        message = message.replace("&A", "<green>").replace("§A", "<green>");
        message = message.replace("&b", "<aqua>").replace("§b", "<aqua>");
        message = message.replace("&B", "<aqua>").replace("§B", "<aqua>");
        message = message.replace("&c", "<red>").replace("§c", "<red>");
        message = message.replace("&C", "<red>").replace("§C", "<red>");
        message = message.replace("&d", "<light_purple>").replace("§d", "<light_purple>");
        message = message.replace("&D", "<light_purple>").replace("§D", "<light_purple>");
        message = message.replace("&e", "<yellow>").replace("§e", "<yellow>");
        message = message.replace("&E", "<yellow>").replace("§E", "<yellow>");
        message = message.replace("&f", "<white>").replace("§f", "<white>");
        message = message.replace("&F", "<white>").replace("§F", "<white>");
        message = message.replace("&k", "<obfuscated>").replace("§k", "<obfuscated>");
        message = message.replace("&K", "<obfuscated>").replace("§K", "<obfuscated>");
        message = message.replace("&l", "<bold>").replace("§l", "<bold>");
        message = message.replace("&L", "<bold>").replace("§L", "<bold>");
        message = message.replace("&m", "<strikethrough>").replace("§m", "<strikethrough>");
        message = message.replace("&M", "<strikethrough>").replace("§M", "<strikethrough>");
        message = message.replace("&n", "<underlined>").replace("§n", "<underlined>");
        message = message.replace("&N", "<underlined>").replace("§N", "<underlined>");
        message = message.replace("&o", "<italic>").replace("§o", "<italic>");
        message = message.replace("&O", "<italic>").replace("§O", "<italic>");
        message = message.replace("&r", "<reset>").replace("§r", "<reset>");
        message = message.replace("&R", "<reset>").replace("§R", "<reset>");

        Matcher hexMatcher = HEX_PATTERN.matcher(message);
        StringBuilder buffer = new StringBuilder();
        while (hexMatcher.find()) {
            String hex = hexMatcher.group(1);
            hexMatcher.appendReplacement(buffer, "<#" + hex + ">");
        }
        hexMatcher.appendTail(buffer);

        return buffer.toString();
    }

    public static String stripColor(String message) {
        if (message == null) {
            return null;
        }
        return PLAIN.serialize(toComponent(message));
    }

    public static String replacePlaceholders(String message, String... replacements) {
        if (message == null || replacements.length == 0) {
            return message;
        }

        String result = message;
        for (int i = 0; i < replacements.length - 1; i += 2) {
            result = result.replace(replacements[i], replacements[i + 1]);
        }
        return result;
    }
}
