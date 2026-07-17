package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Locale;

/** Detects bounded local chat mentions; presentation is supplied by the client adapter. */
public final class MentionNotifier extends Module {
    private final StringSetting mentionTerms;
    private final BooleanSetting includeOwnName;
    private final BooleanSetting caseSensitive;
    private final NumberSetting cooldownSeconds;
    private long lastNotificationAt = Long.MIN_VALUE;

    public MentionNotifier() {
        super("mention_notifier", "MentionNotifier", "Shows local feedback for configured chat mentions.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        mentionTerms = addSetting(new StringSetting("mention_terms", "Mention terms",
                "Comma-separated local words or phrases that trigger a notification.", "", 255, true));
        includeOwnName = addSetting(new BooleanSetting("include_own_name", "Include own name",
                "Also match the local player's name as a whole word.", true));
        caseSensitive = addSetting(new BooleanSetting("case_sensitive", "Case sensitive",
                "Use exact case for local mention terms.", false));
        cooldownSeconds = addSetting(new NumberSetting("cooldown_seconds", "Cooldown",
                "Minimum local time between mention notifications.", 5.0, 1.0, 300.0));
    }

    /** Returns true once per cooldown window when an ordinary remote chat message mentions the player. */
    public boolean shouldNotify(IncomingChatMessage message, String localPlayerName) {
        if (!isEnabled() || message == null || message.channel() != IncomingChatMessage.Channel.CHAT
                || message.text().isBlank() || isSameName(message.sender(), localPlayerName)) {
            return false;
        }
        if (!matchesTerms(message.text(), localPlayerName)) {
            return false;
        }
        long cooldownMillis = Math.round(cooldownSeconds.value() * 1_000.0);
        if (lastNotificationAt != Long.MIN_VALUE && message.receivedAtMillis() - lastNotificationAt < cooldownMillis) {
            return false;
        }
        lastNotificationAt = message.receivedAtMillis();
        return true;
    }

    @Override
    protected void onDisable() {
        lastNotificationAt = Long.MIN_VALUE;
    }

    private boolean matchesTerms(String text, String localPlayerName) {
        if (includeOwnName.value() && isWholeWord(text, localPlayerName, caseSensitive.value())) {
            return true;
        }
        for (String entry : mentionTerms.value().split(",")) {
            String term = entry.trim();
            if (!term.isEmpty() && contains(text, term, caseSensitive.value())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isSameName(String first, String second) {
        return first != null && second != null && !first.isBlank() && first.equalsIgnoreCase(second.trim());
    }

    private static boolean contains(String text, String term, boolean caseSensitive) {
        if (caseSensitive) {
            return text.contains(term);
        }
        return text.toLowerCase(Locale.ROOT).contains(term.toLowerCase(Locale.ROOT));
    }

    private static boolean isWholeWord(String text, String term, boolean caseSensitive) {
        if (term == null || term.isBlank()) {
            return false;
        }
        String subject = caseSensitive ? text : text.toLowerCase(Locale.ROOT);
        String candidate = caseSensitive ? term.trim() : term.trim().toLowerCase(Locale.ROOT);
        int index = subject.indexOf(candidate);
        while (index >= 0) {
            int after = index + candidate.length();
            boolean beforeBoundary = index == 0 || !isNameCharacter(subject.charAt(index - 1));
            boolean afterBoundary = after == subject.length() || !isNameCharacter(subject.charAt(after));
            if (beforeBoundary && afterBoundary) {
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
