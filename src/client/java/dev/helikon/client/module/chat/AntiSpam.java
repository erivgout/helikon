package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** Bounded local repeat and rapid-message policy for incoming chat. */
public final class AntiSpam extends Module {
    private static final int MAX_TRACKED_MESSAGES = 512;
    private static final int MAX_TRACKED_SENDERS = 256;

    public enum Action {
        SHOW,
        HIDE_REPEAT,
        HIDE_RAPID,
        HIDE_JOIN_LEAVE
    }

    /** Repeat count is retained for a future display-level duplicate stacker. */
    public record Decision(Action action, int repeatCount) {
        public Decision {
            if (repeatCount < 1) {
                throw new IllegalArgumentException("repeatCount must be positive");
            }
        }

        public boolean shouldHide() {
            return action != Action.SHOW;
        }
    }

    private final BooleanSetting stackDuplicates;
    private final BooleanSetting hideRepeats;
    private final NumberSetting repeatWindowSeconds;
    private final NumberSetting rapidMessageLimit;
    private final NumberSetting rapidWindowSeconds;
    private final BooleanSetting collapseJoinLeave;
    private final StringSetting whitelistedMessageTypes;
    private final Map<MessageKey, RepeatState> repeats = new LinkedHashMap<>();
    private final Map<String, SenderState> senders = new LinkedHashMap<>();
    private final Map<String, Long> joinLeaveTimes = new LinkedHashMap<>();

    public AntiSpam() {
        super("anti_spam", "AntiSpam", "Locally suppresses bounded duplicate and rapid incoming chat.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        stackDuplicates = addSetting(new BooleanSetting("stack_duplicates", "Stack duplicates",
                "Track local duplicate counts for the chat display stacker.", true));
        hideRepeats = addSetting(new BooleanSetting("hide_repeats", "Hide repeats",
                "Hide an incoming duplicate during the configured local repeat window.", false));
        repeatWindowSeconds = addSetting(new NumberSetting("repeat_window_seconds", "Repeat window",
                "Seconds in which matching local duplicates are counted.", 5.0, 1.0, 60.0));
        rapidMessageLimit = addSetting(new NumberSetting("rapid_message_limit", "Rapid message limit",
                "Maximum messages from one player in the local rapid-message window.", 5.0, 1.0, 20.0));
        rapidWindowSeconds = addSetting(new NumberSetting("rapid_window_seconds", "Rapid message window",
                "Seconds used to count one sender's local rapid messages.", 10.0, 1.0, 60.0));
        collapseJoinLeave = addSetting(new BooleanSetting("collapse_join_leave", "Collapse join/leave",
                "Hide repeated same-type join or leave announcements in the repeat window.", false));
        whitelistedMessageTypes = addSetting(new StringSetting("whitelisted_message_types", "Whitelisted message types",
                "Comma-separated local types to keep: chat, game, death, advancement, join_leave, command_feedback.",
                "", 255, true));
    }

    /** Evaluates one incoming message; no Minecraft objects or display state are required. */
    public Decision evaluate(IncomingChatMessage message) {
        if (!isEnabled() || message == null || isWhitelisted(message)) {
            return new Decision(Action.SHOW, 1);
        }
        long now = message.receivedAtMillis();
        prune(now);
        int repeatCount = countRepeat(message, now);
        boolean rapid = isRapid(message, now);
        boolean repeatedJoinLeave = isRepeatedJoinLeave(message, now);

        if (hideRepeats.value() && repeatCount > 1) {
            return new Decision(Action.HIDE_REPEAT, repeatCount);
        }
        if (rapid) {
            return new Decision(Action.HIDE_RAPID, repeatCount);
        }
        if (repeatedJoinLeave) {
            return new Decision(Action.HIDE_JOIN_LEAVE, repeatCount);
        }
        return new Decision(Action.SHOW, repeatCount);
    }

    /** Whether a display adapter should retain the returned repeat count. */
    public boolean stacksDuplicates() {
        return stackDuplicates.value();
    }

    @Override
    protected void onDisable() {
        repeats.clear();
        senders.clear();
        joinLeaveTimes.clear();
    }

    private int countRepeat(IncomingChatMessage message, long now) {
        MessageKey key = new MessageKey(message.channel(), normalizedSender(message.sender()), normalizedText(message.text()));
        RepeatState prior = repeats.get(key);
        long windowMillis = secondsToMillis(repeatWindowSeconds.value());
        int count = prior != null && now - prior.lastSeenAtMillis < windowMillis ? prior.count + 1 : 1;
        repeats.remove(key);
        repeats.put(key, new RepeatState(now, count));
        trimOldest(repeats, MAX_TRACKED_MESSAGES);
        return count;
    }

    private boolean isRapid(IncomingChatMessage message, long now) {
        if (message.channel() != IncomingChatMessage.Channel.CHAT || !isPlayerName(message.sender())) {
            return false;
        }
        String sender = normalizedSender(message.sender());
        SenderState state = senders.remove(sender);
        if (state == null) {
            state = new SenderState();
        }
        long windowMillis = secondsToMillis(rapidWindowSeconds.value());
        while (!state.messageTimes.isEmpty() && now - state.messageTimes.getFirst() >= windowMillis) {
            state.messageTimes.removeFirst();
        }
        state.messageTimes.addLast(now);
        int maximumTrackedMessages = (int) Math.round(rapidMessageLimit.value()) + 1;
        while (state.messageTimes.size() > maximumTrackedMessages) {
            state.messageTimes.removeFirst();
        }
        state.lastSeenAtMillis = now;
        senders.put(sender, state);
        trimOldest(senders, MAX_TRACKED_SENDERS);
        return state.messageTimes.size() > (int) Math.round(rapidMessageLimit.value());
    }

    /** Package-visible test seam for the bounded rapid-message queue invariant. */
    int trackedRapidMessageCount(String sender) {
        SenderState state = senders.get(normalizedSender(sender));
        return state == null ? 0 : state.messageTimes.size();
    }

    private boolean isRepeatedJoinLeave(IncomingChatMessage message, long now) {
        if (!collapseJoinLeave.value() || !message.isJoinLeaveMessage()) {
            return false;
        }
        String type = message.translationKey();
        Long previous = joinLeaveTimes.remove(type);
        joinLeaveTimes.put(type, now);
        trimOldest(joinLeaveTimes, 8);
        return previous != null && now - previous < secondsToMillis(repeatWindowSeconds.value());
    }

    private boolean isWhitelisted(IncomingChatMessage message) {
        for (String entry : whitelistedMessageTypes.value().split(",")) {
            if (messageType(message).equals(entry.trim().toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static String messageType(IncomingChatMessage message) {
        if (message.isDeathMessage()) {
            return "death";
        }
        if (message.isAdvancementMessage()) {
            return "advancement";
        }
        if (message.isJoinLeaveMessage()) {
            return "join_leave";
        }
        if (message.isCommandFeedback()) {
            return "command_feedback";
        }
        return message.channel() == IncomingChatMessage.Channel.CHAT ? "chat" : "game";
    }

    private void prune(long now) {
        long longestWindow = Math.max(secondsToMillis(repeatWindowSeconds.value()), secondsToMillis(rapidWindowSeconds.value()));
        repeats.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAtMillis >= longestWindow);
        senders.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAtMillis >= longestWindow);
        joinLeaveTimes.entrySet().removeIf(entry -> now - entry.getValue() >= longestWindow);
    }

    private static long secondsToMillis(double seconds) {
        return Math.round(seconds * 1_000.0);
    }

    private static String normalizedSender(String sender) {
        return sender == null ? "" : sender.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizedText(String text) {
        String value = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        return value.length() <= 512 ? value : value.substring(0, 512) + '\u0000' + Integer.toHexString(value.hashCode());
    }

    private static boolean isPlayerName(String value) {
        return value != null && value.trim().matches("[A-Za-z0-9_]{1,16}");
    }

    private static <K, V> void trimOldest(Map<K, V> values, int maximum) {
        Iterator<K> keys = values.keySet().iterator();
        while (values.size() > maximum && keys.hasNext()) {
            keys.next();
            keys.remove();
        }
    }

    private record MessageKey(IncomingChatMessage.Channel channel, String sender, String text) {
    }

    private record RepeatState(long lastSeenAtMillis, int count) {
    }

    private static final class SenderState {
        private final Deque<Long> messageTimes = new ArrayDeque<>();
        private long lastSeenAtMillis;
    }
}
