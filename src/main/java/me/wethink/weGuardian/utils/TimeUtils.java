package me.wethink.weGuardian.utils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimeUtils {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d+)([smhdwMy])");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static long parseTimeToSeconds(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return 0;
        }

        Matcher matcher = TIME_PATTERN.matcher(timeString.toLowerCase());
        long totalSeconds = 0;

        while (matcher.find()) {
            int amount = Integer.parseInt(matcher.group(1));
            String unit = matcher.group(2);

            switch (unit) {
                case "s":
                    totalSeconds += amount;
                    break;
                case "m":
                    totalSeconds += amount * 60L;
                    break;
                case "h":
                    totalSeconds += amount * 3600L;
                    break;
                case "d":
                    totalSeconds += amount * 86400L;
                    break;
                case "w":
                    totalSeconds += amount * 604800L;
                    break;
                case "M":
                    totalSeconds += amount * 2592000L;
                    break;
                case "y":
                    totalSeconds += amount * 31536000L;
                    break;
            }
        }

        return totalSeconds;
    }

    public static LocalDateTime parseTime(String timeString) {
        long seconds = parseTimeToSeconds(timeString);
        if (seconds <= 0) {
            throw new IllegalArgumentException("Invalid time format");
        }
        return LocalDateTime.now().plusSeconds(seconds);
    }

    public static LocalDateTime parseTimeToExpiration(String timeString) {
        long seconds = parseTimeToSeconds(timeString);
        if (seconds <= 0) {
            return null;
        }
        return LocalDateTime.now().plusSeconds(seconds);
    }

    public static String formatDuration(long seconds) {
        if (seconds <= 0) {
            return "Permanent";
        }

        StringBuilder result = new StringBuilder();

        long years = seconds / 31536000;
        if (years > 0) {
            result.append(years).append("y ");
            seconds %= 31536000;
        }

        long months = seconds / 2592000;
        if (months > 0) {
            result.append(months).append("mo ");
            seconds %= 2592000;
        }

        long weeks = seconds / 604800;
        if (weeks > 0) {
            result.append(weeks).append("w ");
            seconds %= 604800;
        }

        long days = seconds / 86400;
        if (days > 0) {
            result.append(days).append("d ");
            seconds %= 86400;
        }

        long hours = seconds / 3600;
        if (hours > 0) {
            result.append(hours).append("h ");
            seconds %= 3600;
        }

        long minutes = seconds / 60;
        if (minutes > 0) {
            result.append(minutes).append("m ");
            seconds %= 60;
        }

        if (seconds > 0) {
            result.append(seconds).append("s");
        }

        return result.toString().trim();
    }

    public static String getRemainingTime(LocalDateTime expiration) {
        if (expiration == null) {
            return "Permanent";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiration)) {
            return "Expired";
        }

        long seconds = ChronoUnit.SECONDS.between(now, expiration);
        return formatDuration(seconds);
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "Never";
        }
        return dateTime.format(FORMATTER);
    }

    public static boolean isValidTimeFormat(String timeString) {
        if (timeString == null || timeString.trim().isEmpty()) {
            return false;
        }

        return timeString.matches("^\\d+[smhdwMy]$");
    }
}
