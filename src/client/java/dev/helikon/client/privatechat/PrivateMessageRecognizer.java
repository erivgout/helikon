package dev.helikon.client.privatechat;

import dev.helikon.client.chat.IncomingChatMessage;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Conservative, Minecraft-free recognition for common incoming private-message display forms. */
public final class PrivateMessageRecognizer {
    private static final String PLAYER = "([A-Za-z0-9_]{1,16})";
    private static final Pattern FROM = Pattern.compile("(?i)^from " + PLAYER + ":\\s*(.+)$");
    private static final Pattern WHISPERS = Pattern.compile("(?i)^" + PLAYER + " whispers to you:\\s*(.+)$");
    private static final Pattern ARROW = Pattern.compile("(?i)^" + PLAYER + "\\s*->\\s*you:\\s*(.+)$");

    private PrivateMessageRecognizer() {
    }

    /** Returns one validated inbound conversation entry, never interpreting arbitrary server text as a command. */
    public static Optional<Incoming> recognize(IncomingChatMessage message, String localPlayerName) {
        if (message == null || message.channel() != IncomingChatMessage.Channel.CHAT) {
            return Optional.empty();
        }
        String text = message.text().trim();
        for (Pattern pattern : new Pattern[]{FROM, WHISPERS, ARROW}) {
            Matcher match = pattern.matcher(text);
            if (match.matches()) {
                String participant = match.group(1);
                String body = match.group(2).trim();
                if (body.isEmpty() || body.length() > 256 || participant.equalsIgnoreCase(normalize(localPlayerName))) {
                    return Optional.empty();
                }
                return Optional.of(new Incoming(participant, body));
            }
        }
        return Optional.empty();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Incoming(String participant, String text) {
        public Incoming {
            participant = PrivateConversation.normalizeParticipant(participant);
            if (text == null || text.isBlank() || text.trim().length() > 256
                    || text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0) {
                throw new IllegalArgumentException("text must be a bounded single-line message");
            }
            text = text.trim();
        }
    }
}
