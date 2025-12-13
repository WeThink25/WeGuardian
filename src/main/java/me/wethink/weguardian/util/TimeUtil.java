package me.wethink.weguardian.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class TimeUtil {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");

    private TimeUtil() {
    }


    public static long parseDuration(String input) {
        if (input == null || input.isEmpty()) {
            return -1;
        }

        if (input.equalsIgnoreCase("permanent") || input.equalsIgnoreCase("perm") || input.equals("-1")) {
            return -1;
        }

        Matcher matcher = TIME_PATTERN.matcher(input.toLowerCase());
        long totalMillis = 0;
        boolean found = false;

        while (matcher.find()) {
            found = true;
            long value = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);

            totalMillis += switch (unit) {
                case "s" -> TimeUnit.SECONDS.toMillis(value);
                case "m" -> TimeUnit.MINUTES.toMillis(value);
                case "h" -> TimeUnit.HOURS.toMillis(value);
                case "d" -> TimeUnit.DAYS.toMillis(value);
                case "w" -> TimeUnit.DAYS.toMillis(value * 7);
                case "M" -> TimeUnit.DAYS.toMillis(value * 30);
                case "y" -> TimeUnit.DAYS.toMillis(value * 365);
                default -> 0;
            };
        }

        return found ? totalMillis : -1;
    }


    public static String formatDuration(long millis) {
        if (millis < 0) {
            return "Permanent";
        }

        if (millis == 0) {
            return "Expired";
        }

        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();

        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0) {
            sb.append(minutes).append("m ");
        }
        if (seconds > 0 && days == 0) {
            sb.append(seconds).append("s");
        }

        String result = sb.toString().trim();
        return result.isEmpty() ? "< 1s" : result;
    }


    public static String formatRemaining(Instant expiresAt) {
        if (expiresAt == null) {
            return "Permanent";
        }

        long remaining = expiresAt.toEpochMilli() - Instant.now().toEpochMilli();
        if (remaining <= 0) {
            return "Expired";
        }

        return formatDuration(remaining);
    }


    public static String formatDate(Instant instant) {
        if (instant == null) {
            return "Never";
        }

        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter
                .ofPattern("MMM dd, yyyy HH:mm")
                .withZone(java.time.ZoneId.systemDefault());

        return formatter.format(instant);
    }


    public static Instant getExpiryInstant(long durationMillis) {
        if (durationMillis < 0) {
            return null;
        }
        return Instant.now().plusMillis(durationMillis);
    }
}
