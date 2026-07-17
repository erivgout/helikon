package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiSpamTest {
    @Test
    void countsAndOptionallyHidesDuplicateMessagesWithinTheRepeatWindow() {
        AntiSpam antiSpam = enabled();
        booleanSetting(antiSpam, "hide_repeats").set(true);

        assertEquals(new AntiSpam.Decision(AntiSpam.Action.SHOW, 1), antiSpam.evaluate(chat("Bob", "Hello", 1_000L)));
        assertEquals(new AntiSpam.Decision(AntiSpam.Action.HIDE_REPEAT, 2), antiSpam.evaluate(chat("bob", "hello", 2_000L)));
        assertEquals(new AntiSpam.Decision(AntiSpam.Action.SHOW, 1), antiSpam.evaluate(chat("Bob", "hello", 7_000L)));
        assertTrue(antiSpam.stacksDuplicates());
    }

    @Test
    void limitsRapidPlayerChatButNotUnnamedGameMessages() {
        AntiSpam antiSpam = enabled();
        numberSetting(antiSpam, "rapid_message_limit").set(2.0);
        numberSetting(antiSpam, "rapid_window_seconds").set(10.0);

        assertFalse(antiSpam.evaluate(chat("Bob", "one", 1_000L)).shouldHide());
        assertFalse(antiSpam.evaluate(chat("Bob", "two", 2_000L)).shouldHide());
        assertEquals(AntiSpam.Action.HIDE_RAPID, antiSpam.evaluate(chat("Bob", "three", 3_000L)).action());
        assertFalse(antiSpam.evaluate(game("Server warning", 4_000L)).shouldHide());
    }

    @Test
    void capsOneSendersRapidTimestampQueueDuringAMessageFlood() {
        AntiSpam antiSpam = enabled();
        numberSetting(antiSpam, "rapid_message_limit").set(2.0);
        for (int index = 0; index < 1_000; index++) {
            antiSpam.evaluate(chat("Bob", "message-" + index, 1_000L + index));
        }

        assertEquals(3, antiSpam.trackedRapidMessageCount("Bob"));
        assertEquals(AntiSpam.Action.HIDE_RAPID, antiSpam.evaluate(chat("Bob", "final", 2_001L)).action());
    }

    @Test
    void collapsesSameTypeJoinLeaveAnnouncementsAndHonorsWhitelistedTypes() {
        AntiSpam antiSpam = enabled();
        booleanSetting(antiSpam, "collapse_join_leave").set(true);
        IncomingChatMessage joined = new IncomingChatMessage(IncomingChatMessage.Channel.GAME, "Bob joined", "",
                "multiplayer.player.joined", false, 1_000L);
        IncomingChatMessage joinedAgain = new IncomingChatMessage(IncomingChatMessage.Channel.GAME, "Eve joined", "",
                "multiplayer.player.joined", false, 2_000L);
        assertFalse(antiSpam.evaluate(joined).shouldHide());
        assertEquals(AntiSpam.Action.HIDE_JOIN_LEAVE, antiSpam.evaluate(joinedAgain).action());

        stringSetting(antiSpam, "whitelisted_message_types").set("join_leave");
        assertFalse(antiSpam.evaluate(new IncomingChatMessage(IncomingChatMessage.Channel.GAME, "Ann joined", "",
                "multiplayer.player.joined", false, 3_000L)).shouldHide());
    }

    @Test
    void resetsTrackingWhenDisabled() {
        AntiSpam antiSpam = enabled();
        booleanSetting(antiSpam, "hide_repeats").set(true);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiSpam);
        registry.setEnabled(antiSpam, true);
        antiSpam.evaluate(chat("Bob", "hello", 1_000L));
        registry.setEnabled(antiSpam, false);
        registry.setEnabled(antiSpam, true);

        assertEquals(new AntiSpam.Decision(AntiSpam.Action.SHOW, 1), antiSpam.evaluate(chat("Bob", "hello", 2_000L)));
    }

    private static AntiSpam enabled() {
        AntiSpam antiSpam = new AntiSpam();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(antiSpam);
        registry.setEnabled(antiSpam, true);
        return antiSpam;
    }

    private static IncomingChatMessage chat(String sender, String text, long at) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, text, sender, "", false, at);
    }

    private static IncomingChatMessage game(String text, long at) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.GAME, text, "", "", false, at);
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Module module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static StringSetting stringSetting(Module module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
