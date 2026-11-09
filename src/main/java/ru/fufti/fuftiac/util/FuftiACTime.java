package ru.fufti.fuftiac.util;

import java.time.Duration;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class FuftiACTime {

    private static final Pattern PATTERN = Pattern.compile("(\\d+)\\s*([smhdw])", Pattern.CASE_INSENSITIVE);

    private FuftiACTime() {
    }

    public static Duration parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        String raw = input.trim().toLowerCase(Locale.ROOT);
        if (raw.equals("perm") || raw.equals("permanent") || raw.equals("forever")) {
            return Duration.ZERO;
        }
        Matcher matcher = PATTERN.matcher(raw);
        if (!matcher.matches()) {
            return null;
        }
        long amount = Long.parseLong(matcher.group(1));
        return switch (matcher.group(2).toLowerCase(Locale.ROOT)) {
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            case "w" -> Duration.ofDays(amount * 7);
            default -> null;
        };
    }

    public static String pretty(Duration duration) {
        if (duration == null || duration.isZero()) {
            return "навсегда";
        }
        long seconds = duration.getSeconds();
        if (seconds % 86400 == 0) {
            return (seconds / 86400) + "d";
        }
        if (seconds % 3600 == 0) {
            return (seconds / 3600) + "h";
        }
        if (seconds % 60 == 0) {
            return (seconds / 60) + "m";
        }
        return seconds + "s";
    }
}
