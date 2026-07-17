package dev.helikon.client.chat;

import java.util.Locale;

/** Minecraft-free classification for conservative whole-line local chat coloring. */
public final class ChatColorPolicy {
    public enum MessageType {
        NORMAL,
        SYSTEM,
        MENTION,
        PRIVATE_MESSAGE
    }

    private ChatColorPolicy() {
    }

    public static MessageType classify(String text, boolean serverSystem, String localPlayerName) {
        if (serverSystem) {
            return MessageType.SYSTEM;
        }
        String normalized = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        if (looksPrivateMessage(normalized)) {
            return MessageType.PRIVATE_MESSAGE;
        }
        if (containsWholeName(text, localPlayerName)) {
            return MessageType.MENTION;
        }
        return MessageType.NORMAL;
    }

    private static boolean looksPrivateMessage(String text) {
        return text.startsWith("[pm]") || text.startsWith("[msg]") || text.startsWith("from ")
                || text.startsWith("to ") || text.startsWith("whisper from ") || text.startsWith("whisper to ");
    }

    private static boolean containsWholeName(String text, String name) {
        if (text == null || name == null || name.isBlank()) {
            return false;
        }
        String subject = text.toLowerCase(Locale.ROOT);
        String candidate = name.trim().toLowerCase(Locale.ROOT);
        int index = subject.indexOf(candidate);
        while (index >= 0) {
            int end = index + candidate.length();
            boolean before = index == 0 || !isNameCharacter(subject.charAt(index - 1));
            boolean after = end == subject.length() || !isNameCharacter(subject.charAt(end));
            if (before && after) {
                return true;
            }
            index = subject.indexOf(candidate, index + 1);
        }
        return false;
    }

    private static boolean isNameCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }
}
