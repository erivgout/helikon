package dev.helikon.client.chat;

/** Validates a standard player-name span before BetterChat attaches a local action. */
public final class ChatPlayerNamePolicy {
    private ChatPlayerNamePolicy() {
    }

    public static boolean isVanillaPlayerName(String value) {
        return value != null && value.matches("[A-Za-z0-9_]{1,16}");
    }
}
