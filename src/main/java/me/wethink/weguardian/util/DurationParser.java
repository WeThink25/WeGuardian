package me.wethink.weguardian.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class DurationParser {

    private static final Pattern DURATION_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");


    public static long parse(String duration) {
        if (duration == null || duration.isEmpty()) {
            return -1;
        }

        Matcher matcher = DURATION_PATTERN.matcher(duration.trim());
        if (!matcher.matches()) {
            return -1;
        }

        long amount = Long.parseLong(matcher.group(1));
        char unit = matcher.group(2).charAt(0);

        return switch (unit) {
            case 's' -> amount * 1000L;
            case 'm' -> amount * 60 * 1000L;
            case 'h' -> amount * 60 * 60 * 1000L;
            case 'd' -> amount * 24 * 60 * 60 * 1000L;
            case 'w' -> amount * 7 * 24 * 60 * 60 * 1000L;
            case 'M' -> amount * 30 * 24 * 60 * 60 * 1000L;
            case 'y' -> amount * 365 * 24 * 60 * 60 * 1000L;
            default -> -1;
        };
    }


    public static String format(long millis) {
        if (millis <= 0) {
            return "Permanent";
        }

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return days + " day" + (days != 1 ? "s" : "");
        } else if (hours > 0) {
            return hours + " hour" + (hours != 1 ? "s" : "");
        } else if (minutes > 0) {
            return minutes + " minute" + (minutes != 1 ? "s" : "");
        } else {
            return seconds + " second" + (seconds != 1 ? "s" : "");
        }
    }
}
