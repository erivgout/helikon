package dev.helikon.client.module.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatSpammerTest {
    @Test
    void sendsSequentialOrdinaryMessagesAtTheMinimumConfiguredInterval() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(sender, () -> 0);
        stringSetting(spammer, "messages").set("/command;.local;first;second");
        numberSetting(spammer, "delay_ticks").set((double) ChatSpammer.MINIMUM_DELAY_TICKS);
        numberSetting(spammer, "session_message_cap").set(2.0);

        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));
        assertEquals(List.of("first"), sender.messages);
        for (int tick = 0; tick < ChatSpammer.MINIMUM_DELAY_TICKS; tick++) {
            spammer.tick(true, false);
        }
        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));
        assertEquals(List.of("first", "second"), sender.messages);
        assertEquals(ChatSpammer.Action.STOPPED, spammer.tick(true, false));
    }

    @Test
    void supportsInjectedRandomOrderAndPausesForScreens() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(sender, () -> 5);
        stringSetting(spammer, "messages").set("first;second;third");
        booleanSetting(spammer, "random_order").set(true);

        assertEquals(ChatSpammer.Action.NONE, spammer.tick(true, true));
        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));
        assertEquals(List.of("third"), sender.messages);
    }

    @Test
    void stopsAfterDisconnectUntilExplicitlyReenabled() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(sender, () -> 0);
        stringSetting(spammer, "messages").set("hello");
        assertEquals(ChatSpammer.Action.STOPPED, spammer.tick(false, false));
        assertEquals(ChatSpammer.Action.NONE, spammer.tick(true, false));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(spammer);
        registry.setEnabled(spammer, false);
        registry.setEnabled(spammer, true);
        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));
    }

    @Test
    void retainsCooldownAcrossToggleAndStopsAfterThreeObservedCancellations() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(sender, () -> 0);
        stringSetting(spammer, "messages").set("hello");
        numberSetting(spammer, "delay_ticks").set((double) ChatSpammer.MINIMUM_DELAY_TICKS);
        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(spammer);
        registry.setEnabled(spammer, false);
        registry.setEnabled(spammer, true);
        assertEquals(ChatSpammer.Action.NONE, spammer.tick(true, false));

        RejectingSender rejectingSender = new RejectingSender();
        ChatSpammer rejecting = enabled(rejectingSender, () -> 0);
        stringSetting(rejecting, "messages").set("blocked");
        numberSetting(rejecting, "delay_ticks").set((double) ChatSpammer.MINIMUM_DELAY_TICKS);
        rejectingSender.spammer = rejecting;
        assertEquals(ChatSpammer.Action.SENT, rejecting.tick(true, false));
        advanceMinimumDelay(rejecting);
        assertEquals(ChatSpammer.Action.SENT, rejecting.tick(true, false));
        advanceMinimumDelay(rejecting);
        assertEquals(ChatSpammer.Action.STOPPED, rejecting.tick(true, false));
    }

    @Test
    void neverSendsWhenEveryConfiguredEntryIsCommandLike() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(sender, () -> 0);
        stringSetting(spammer, "messages").set("/msg Person hi;.help");

        assertEquals(ChatSpammer.Action.STOPPED, spammer.tick(true, false));
        assertEquals(List.of(), sender.messages);
    }

    @Test
    void appendsSessionCounterAndInjectedTimestampSuffixes() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(new ChatSpammer(sender, () -> 0, () -> "12:34:56"));
        stringSetting(spammer, "messages").set("hello");
        numberSetting(spammer, "delay_ticks").set((double) ChatSpammer.MINIMUM_DELAY_TICKS);
        booleanSetting(spammer, "counter").set(true);
        booleanSetting(spammer, "timestamps").set(true);

        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));
        advanceMinimumDelay(spammer);
        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));

        assertEquals(List.of("hello [1] [12:34:56]", "hello [2] [12:34:56]"), sender.messages);
    }

    @Test
    void truncatesDecoratedMessagesToTheOrdinaryChatLimit() {
        RecordingSender sender = new RecordingSender();
        ChatSpammer spammer = enabled(new ChatSpammer(sender, () -> 0, () -> "12:34:56"));
        stringSetting(spammer, "messages").set("a".repeat(255));
        booleanSetting(spammer, "counter").set(true);
        booleanSetting(spammer, "timestamps").set(true);

        assertEquals(ChatSpammer.Action.SENT, spammer.tick(true, false));

        String sent = sender.messages.get(0);
        assertEquals(256, sent.length());
        assertTrue(sent.endsWith(" [1] [12:34:56]"));
    }

    private static ChatSpammer enabled(ChatSpammer.ChatSender sender, java.util.function.IntSupplier random) {
        return enabled(new ChatSpammer(sender, random));
    }

    private static ChatSpammer enabled(ChatSpammer spammer) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(spammer);
        registry.setEnabled(spammer, true);
        return spammer;
    }

    private static void advanceMinimumDelay(ChatSpammer spammer) {
        for (int tick = 0; tick < ChatSpammer.MINIMUM_DELAY_TICKS; tick++) {
            spammer.tick(true, false);
        }
    }

    private static StringSetting stringSetting(ChatSpammer module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(ChatSpammer module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static BooleanSetting booleanSetting(ChatSpammer module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static final class RecordingSender implements ChatSpammer.ChatSender {
        private final List<String> messages = new ArrayList<>();

        @Override
        public void send(String message) {
            messages.add(message);
        }
    }

    private static final class RejectingSender implements ChatSpammer.ChatSender {
        private ChatSpammer spammer;

        @Override
        public void send(String message) {
            spammer.reportRejected(message);
        }
    }
}
