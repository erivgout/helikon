package dev.helikon.client.chat;

import java.util.Locale;
import java.util.Objects;

/** Normalized incoming-chat facts, isolated from Minecraft's component types. */
public record IncomingChatMessage(Channel channel, String text, String rawText, String sender, String translationKey,
                                  boolean overlay, long receivedAtMillis) {
    public enum Channel {
        CHAT,
        GAME
    }

    public IncomingChatMessage {
        channel = Objects.requireNonNull(channel, "channel");
        text = Objects.requireNonNull(text, "text");
        rawText = rawText == null ? text : rawText;
        sender = sender == null ? "" : sender.trim();
        translationKey = translationKey == null ? "" : translationKey.trim().toLowerCase(Locale.ROOT);
        if (receivedAtMillis < 0) {
            throw new IllegalArgumentException("receivedAtMillis must not be negative");
        }
    }

    /** Compatibility constructor for policies that have no separate chat body. */
    public IncomingChatMessage(Channel channel, String text, String sender, String translationKey,
                               boolean overlay, long receivedAtMillis) {
        this(channel, text, text, sender, translationKey, overlay, receivedAtMillis);
    }

    public boolean isDeathMessage() {
        return translationKey.startsWith("death.");
    }

    public boolean isAdvancementMessage() {
        return translationKey.startsWith("chat.type.advancement.");
    }

    public boolean isJoinLeaveMessage() {
        return translationKey.equals("multiplayer.player.joined") || translationKey.equals("multiplayer.player.left");
    }

    public boolean isCommandFeedback() {
        return translationKey.startsWith("commands.") || translationKey.startsWith("advmode.");
    }
}
