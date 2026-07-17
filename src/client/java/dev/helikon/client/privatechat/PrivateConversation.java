package dev.helikon.client.privatechat;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** One bounded, in-memory local private-message conversation tab. */
public record PrivateConversation(String participant, List<Entry> entries) {
    public enum Direction {
        OUTGOING,
        INCOMING
    }

    public record Entry(Direction direction, String text, long sequence) {
        public Entry {
            direction = Objects.requireNonNull(direction, "direction");
            text = requireText(text, "text");
            if (sequence < 0) {
                throw new IllegalArgumentException("sequence must not be negative");
            }
        }
    }

    public PrivateConversation {
        participant = normalizeParticipant(participant);
        entries = List.copyOf(entries);
    }

    public static String normalizeParticipant(String value) {
        String normalized = requireText(value, "participant");
        if (!normalized.matches("[A-Za-z0-9_]{1,16}")) {
            throw new IllegalArgumentException("participant must be a Minecraft-style player name");
        }
        return normalized;
    }

    public String key() {
        return participant.toLowerCase(Locale.ROOT);
    }

    private static String requireText(String value, String field) {
        String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty() || checked.length() > 256 || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException(field + " must be bounded single-line text");
        }
        return checked;
    }
}
