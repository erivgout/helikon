package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Sends configured ordinary chat only at a conservative local interval and session cap. */
public final class ChatSpammer extends Module {
    public enum Action {
        NONE,
        SENT,
        STOPPED
    }

    /** Two seconds at 20 TPS; deliberately above an ordinary client tick burst. */
    public static final int MINIMUM_DELAY_TICKS = 40;
    public static final int REJECTION_STOP_THRESHOLD = 3;
    private static final int MAX_CHAT_LENGTH = 256;
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ChatSender sender;
    private final IntSupplier randomIndex;
    private final Supplier<String> timestampText;
    private final StringSetting messages;
    private final NumberSetting delayTicks;
    private final BooleanSetting randomOrder;
    private final BooleanSetting pauseInGui;
    private final BooleanSetting counter;
    private final BooleanSetting timestamps;
    private final NumberSetting sessionMessageCap;
    private int ticksUntilSend;
    private int sentThisSession;
    private int nextSequentialMessage;
    private boolean stoppedForDisconnect;
    private boolean stoppedForRejections;
    private int rejectedMessages;
    private boolean awaitingCancellation;
    private String activeMessage = "";

    public ChatSpammer(ChatSender sender, IntSupplier randomIndex) {
        this(sender, randomIndex, () -> LocalTime.now().format(TIMESTAMP_FORMAT));
    }

    public ChatSpammer(ChatSender sender, IntSupplier randomIndex, Supplier<String> timestampText) {
        super("chat_spammer", "ChatSpammer", "Sends capped, delayed ordinary chat; servers may punish spam.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        this.sender = Objects.requireNonNull(sender, "sender");
        this.randomIndex = Objects.requireNonNull(randomIndex, "randomIndex");
        this.timestampText = Objects.requireNonNull(timestampText, "timestampText");
        messages = addSetting(new StringSetting("messages", "Messages",
                "Semicolon-separated ordinary chat messages; commands are rejected.", "", 255, true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum local delay between messages in ticks.",
                100.0, MINIMUM_DELAY_TICKS, 1_200.0));
        randomOrder = addSetting(new BooleanSetting("random_order", "Random order",
                "Choose a random configured message instead of sequential order.", false));
        pauseInGui = addSetting(new BooleanSetting("pause_in_gui", "Pause in GUI",
                "Pause sending while any screen is open.", true));
        counter = addSetting(new BooleanSetting("counter", "Counter",
                "Append this session's message number to each sent message.", false));
        timestamps = addSetting(new BooleanSetting("timestamps", "Timestamps",
                "Append the local send time to each sent message.", false));
        sessionMessageCap = addSetting(new NumberSetting("session_message_cap", "Session message cap",
                "Maximum messages sent until the client is restarted.", 10.0, 1.0, 100.0));
    }

    /** Evaluates one client tick. The sender is the existing normal Minecraft chat path. */
    public Action tick(boolean playerAvailable, boolean screenOpen) {
        if (!playerAvailable) {
            stoppedForDisconnect = true;
            ticksUntilSend = 0;
            return Action.STOPPED;
        }
        if (!isEnabled() || stoppedForDisconnect || stoppedForRejections || (screenOpen && pauseInGui.value())) {
            return Action.NONE;
        }
        List<String> availableMessages = configuredMessages();
        if (availableMessages.isEmpty() || sentThisSession >= (int) Math.round(sessionMessageCap.value())) {
            return Action.STOPPED;
        }
        if (ticksUntilSend > 0) {
            ticksUntilSend--;
            return Action.NONE;
        }

        String message = decorate(selectMessage(availableMessages));
        awaitingCancellation = true;
        activeMessage = message;
        try {
            sender.send(message);
        } finally {
            awaitingCancellation = false;
            activeMessage = "";
        }
        if (stoppedForRejections) {
            return Action.STOPPED;
        }
        sentThisSession++;
        ticksUntilSend = (int) Math.round(delayTicks.value());
        return Action.SENT;
    }

    public int sentThisSession() {
        return sentThisSession;
    }

    /** Records a synchronous normal-chat cancellation for the message this module is sending. */
    public void reportRejected(String message) {
        if (!awaitingCancellation || !activeMessage.equals(message)) {
            return;
        }
        rejectedMessages++;
        if (rejectedMessages >= REJECTION_STOP_THRESHOLD) {
            stoppedForRejections = true;
        }
    }

    @Override
    protected void onEnable() {
        stoppedForDisconnect = false;
    }

    private List<String> configuredMessages() {
        List<String> parsed = new ArrayList<>();
        for (String entry : messages.value().split(";")) {
            String message = entry.trim();
            if (!message.isEmpty() && message.length() <= MAX_CHAT_LENGTH
                    && !message.startsWith("/") && !message.startsWith(".")) {
                parsed.add(message);
            }
        }
        return List.copyOf(parsed);
    }

    /** Appends the optional counter/timestamp suffixes within the ordinary 256-character chat limit. */
    private String decorate(String message) {
        StringBuilder suffix = new StringBuilder();
        if (counter.value()) {
            suffix.append(" [").append(sentThisSession + 1).append(']');
        }
        if (timestamps.value()) {
            suffix.append(" [").append(timestampText.get()).append(']');
        }
        if (suffix.isEmpty()) {
            return message;
        }
        int available = Math.max(0, MAX_CHAT_LENGTH - suffix.length());
        String base = message.length() > available ? message.substring(0, available) : message;
        return base + suffix;
    }

    private String selectMessage(List<String> availableMessages) {
        if (randomOrder.value()) {
            return availableMessages.get(Math.floorMod(randomIndex.getAsInt(), availableMessages.size()));
        }
        String selected = availableMessages.get(nextSequentialMessage % availableMessages.size());
        nextSequentialMessage = (nextSequentialMessage + 1) % availableMessages.size();
        return selected;
    }

    /** Narrow port to Minecraft's normal outgoing chat sender. */
    public interface ChatSender {
        void send(String message);
    }
}
