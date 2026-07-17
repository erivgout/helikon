package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.TextMatchRules;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Evaluates one bounded local automatic-reply rule without Minecraft dependencies. */
public final class AutoReply extends Module {
    private static final long ONE_MINUTE_MILLIS = 60_000L;
    private static final long MAXIMUM_COOLDOWN_MILLIS = 300_000L;
    private static final int MAXIMUM_REMEMBERED_REPLIES = 16;

    public enum MatchMode {
        EXACT,
        CONTAINS,
        REGEX
    }

    private final StringSetting trigger;
    private final EnumSetting<MatchMode> matchMode;
    private final StringSetting reply;
    private final NumberSetting cooldownSeconds;
    private final StringSetting whitelist;
    private final StringSetting blacklist;
    private final StringSetting serverRestriction;
    private final NumberSetting repliesPerMinute;
    private final BooleanSetting pauseInGui;
    private final Map<String, Long> lastReplyBySender = new HashMap<>();
    private final Deque<Long> replyTimes = new ArrayDeque<>();
    private final Deque<ReplyFingerprint> recentReplyTexts = new ArrayDeque<>();

    public AutoReply() {
        super("auto_reply", "AutoReply", "Sends a bounded local reply to one safe incoming-chat rule.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        trigger = addSetting(new StringSetting("trigger", "Trigger",
                "Text or a bounded regex that permits the local reply.", "", 96, true));
        matchMode = addSetting(new EnumSetting<>("match_mode", "Match mode",
                "Choose exact, contains, or safe-regex trigger matching.", MatchMode.class, MatchMode.CONTAINS));
        reply = addSetting(new StringSetting("reply", "Reply",
                "Ordinary single-line chat sent after a matching incoming message.", "", 256, true));
        cooldownSeconds = addSetting(new NumberSetting("cooldown_seconds", "Sender cooldown",
                "Minimum seconds before replying to the same sender again.", 30.0, 5.0, 300.0));
        whitelist = addSetting(new StringSetting("whitelist", "Whitelist",
                "Optional comma-separated player names allowed to trigger replies.", "", 255, true));
        blacklist = addSetting(new StringSetting("blacklist", "Blacklist",
                "Comma-separated player names that never trigger replies.", "", 255, true));
        serverRestriction = addSetting(new StringSetting("server_restriction", "Server restriction",
                "Optional exact local multiplayer address allowed to trigger replies.", "", 255, true));
        repliesPerMinute = addSetting(new NumberSetting("replies_per_minute", "Replies per minute",
                "Maximum automatic replies in any rolling local minute.", 3.0, 1.0, 10.0));
        pauseInGui = addSetting(new BooleanSetting("pause_in_gui", "Pause in GUI",
                "Do not send automatic replies while any screen is open.", true));
    }

    /** Returns a reply only when the complete local safety policy permits one. */
    public Optional<String> replyFor(IncomingChatMessage message, String localPlayerName,
                                     String currentServerAddress, boolean screenOpen) {
        if (!isEnabled() || message == null || (screenOpen && pauseInGui.value())
                || message.channel() != IncomingChatMessage.Channel.CHAT
                || !isValidPlayerName(message.sender()) || isSameName(message.sender(), localPlayerName)
                || !isValidReply() || !matchesServer(currentServerAddress) || !matchesSender(message.sender())
                || !matchesTrigger(message.rawText())) {
            return Optional.empty();
        }

        long now = message.receivedAtMillis();
        prune(now);
        if (isRecentReplyText(message.rawText())) {
            return Optional.empty();
        }
        String senderKey = message.sender().toLowerCase(Locale.ROOT);
        long cooldownMillis = Math.round(cooldownSeconds.value() * 1_000.0);
        Long lastReply = lastReplyBySender.get(senderKey);
        if (lastReply != null && now - lastReply < cooldownMillis) {
            return Optional.empty();
        }
        if (replyTimes.size() >= (int) Math.round(repliesPerMinute.value())) {
            return Optional.empty();
        }

        String value = reply.value().trim();
        lastReplyBySender.put(senderKey, now);
        replyTimes.addLast(now);
        recentReplyTexts.addLast(new ReplyFingerprint(normalizeText(value), now));
        while (recentReplyTexts.size() > MAXIMUM_REMEMBERED_REPLIES) {
            recentReplyTexts.removeFirst();
        }
        return Optional.of(value);
    }

    @Override
    protected void onDisable() {
        lastReplyBySender.clear();
        replyTimes.clear();
        recentReplyTexts.clear();
    }

    private boolean matchesTrigger(String text) {
        String configured = trigger.value().trim();
        if (configured.isEmpty() || text == null || text.isBlank()) {
            return false;
        }
        return switch (matchMode.value()) {
            case EXACT -> text.equalsIgnoreCase(configured);
            case CONTAINS -> text.toLowerCase(Locale.ROOT).contains(configured.toLowerCase(Locale.ROOT));
            case REGEX -> !configured.contains(";") && TextMatchRules.matchesRegex(text, configured, false);
        };
    }

    private boolean matchesSender(String sender) {
        if (containsPlayerName(blacklist.value(), sender)) {
            return false;
        }
        return whitelist.value().isBlank() || containsPlayerName(whitelist.value(), sender);
    }

    private boolean matchesServer(String currentServerAddress) {
        String configured = serverRestriction.value();
        String required = normalizeServer(configured);
        if (!configured.isBlank() && required.isEmpty()) {
            return false;
        }
        return required.isEmpty() || required.equals(normalizeServer(currentServerAddress));
    }

    private boolean isValidReply() {
        String value = reply.value().trim();
        return !value.isEmpty() && value.length() <= 256 && value.indexOf('\n') < 0 && value.indexOf('\r') < 0
                && !value.startsWith(".") && !value.startsWith("/");
    }

    private boolean isRecentReplyText(String text) {
        String normalized = normalizeText(text);
        return recentReplyTexts.stream().anyMatch(reply -> reply.text().equals(normalized));
    }

    private void prune(long now) {
        while (!replyTimes.isEmpty() && now - replyTimes.getFirst() >= ONE_MINUTE_MILLIS) {
            replyTimes.removeFirst();
        }
        lastReplyBySender.entrySet().removeIf(entry -> now - entry.getValue() >= MAXIMUM_COOLDOWN_MILLIS);
        while (!recentReplyTexts.isEmpty()
                && now - recentReplyTexts.getFirst().sentAtMillis() >= MAXIMUM_COOLDOWN_MILLIS) {
            recentReplyTexts.removeFirst();
        }
    }

    private static boolean containsPlayerName(String entries, String sender) {
        for (String entry : entries.split(",")) {
            if (isValidPlayerName(entry.trim()) && entry.trim().equalsIgnoreCase(sender)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isValidPlayerName(String value) {
        return value != null && value.trim().matches("[A-Za-z0-9_]{1,16}");
    }

    private static boolean isSameName(String first, String second) {
        return first != null && second != null && !second.isBlank() && first.equalsIgnoreCase(second.trim());
    }

    private static String normalizeServer(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        return normalized.length() <= 255 && normalized.chars().noneMatch(Character::isISOControl) ? normalized : "";
    }

    private static String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private record ReplyFingerprint(String text, long sentAtMillis) {
    }
}
