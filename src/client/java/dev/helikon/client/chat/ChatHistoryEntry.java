package dev.helikon.client.chat;

import java.util.Objects;

/** One bounded local chat record, independent of Minecraft component classes. */
public record ChatHistoryEntry(long timestampMillis, Direction direction, String sender, String text) {
    public enum Direction {
        INCOMING,
        OUTGOING
    }

    public static final int MAXIMUM_TEXT_LENGTH = 1_024;

    public ChatHistoryEntry {
        if (timestampMillis < 0L) {
            throw new IllegalArgumentException("timestampMillis must be non-negative");
        }
        direction = Objects.requireNonNull(direction, "direction");
        sender = normalizeSender(sender);
        text = requireText(text);
    }

    public boolean canCopyPlayerName() {
        return !sender.isEmpty();
    }

    public boolean canReopen() {
        return direction == Direction.OUTGOING;
    }

    private static String normalizeSender(String value) {
        String candidate = value == null ? "" : value.trim();
        if (candidate.isEmpty()) {
            return "";
        }
        if (!candidate.matches("[A-Za-z0-9_]{3,16}")) {
            return "";
        }
        return candidate;
    }

    private static String requireText(String value) {
        String text = Objects.requireNonNull(value, "text").trim();
        if (text.isEmpty() || text.length() > MAXIMUM_TEXT_LENGTH
                || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Chat history text must be a bounded single line");
        }
        return text;
    }
}
