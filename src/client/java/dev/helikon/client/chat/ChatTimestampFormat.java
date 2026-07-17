package dev.helikon.client.chat;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Minecraft-free local timestamp-label formatter. */
public final class ChatTimestampFormat {
    private ChatTimestampFormat() {
    }

    public static String format(Instant timestamp, Instant sessionStart, ZoneId zone, boolean twentyFourHour,
                                boolean includeSeconds, boolean brackets, boolean relative) {
        if (timestamp == null || sessionStart == null || zone == null) {
            throw new IllegalArgumentException("timestamp, sessionStart, and zone must not be null");
        }
        String value;
        if (relative) {
            value = relativeLabel(Duration.between(sessionStart, timestamp));
        } else {
            String pattern = twentyFourHour ? (includeSeconds ? "HH:mm:ss" : "HH:mm")
                    : (includeSeconds ? "hh:mm:ss a" : "hh:mm a");
            value = DateTimeFormatter.ofPattern(pattern, Locale.ROOT).withZone(zone).format(timestamp);
        }
        return brackets ? "[" + value + "] " : value + " ";
    }

    private static String relativeLabel(Duration elapsed) {
        long seconds = Math.max(0L, elapsed.getSeconds());
        if (seconds < 60L) {
            return "+" + seconds + "s";
        }
        if (seconds < 3_600L) {
            return "+" + seconds / 60L + "m";
        }
        return "+" + seconds / 3_600L + "h";
    }
}
