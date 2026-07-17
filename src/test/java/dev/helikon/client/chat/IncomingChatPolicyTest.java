package dev.helikon.client.chat;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatFilter;
import dev.helikon.client.module.chat.ChatMute;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomingChatPolicyTest {
    @Test
    void chatMuteUsesStructuredCategoriesCustomTextAndConsecutiveDuplicates() {
        ChatMute mute = enabled(new ChatMute());
        booleanSetting(mute, "death_messages").set(true);
        booleanSetting(mute, "repeated_messages").set(true);
        stringSetting(mute, "custom_text_filters").set("spoiler");

        assertTrue(mute.shouldHide(game("Player fell", "death.fell.accident.generic", false, 1)));
        assertTrue(mute.shouldHide(chat("contains SPOILER", "Alex", 2)));
        assertFalse(mute.shouldHide(chat("same", "Alex", 3)));
        assertTrue(mute.shouldHide(chat("same", "Alex", 4)));
    }

    @Test
    void chatFilterMatchesKeywordsPlayersAndSafeRegexWithoutCaseSensitivity() {
        ChatFilter filter = enabled(new ChatFilter());
        stringSetting(filter, "keyword_filters").set("sale,spoiler");
        stringSetting(filter, "player_filters").set("griefer");
        stringSetting(filter, "regex_filters").set("offer\\s+\\d+");

        assertTrue(filter.shouldHide(chat("Big SALE today", "Alex", 1)));
        assertTrue(filter.shouldHide(chat("hello", "Griefer", 2)));
        assertTrue(filter.shouldHide(chat("special offer 20", "Alex", 3)));
        assertFalse(filter.shouldHide(chat("ordinary message", "Alex", 4)));
    }

    @Test
    void rejectsUnsafeOrMalformedRegexByTreatingTheFilterAsNoMatch() {
        ChatFilter filter = enabled(new ChatFilter());
        stringSetting(filter, "regex_filters").set("(a+)+;(a|aa)+$;(a|a?)+$;[");

        assertFalse(filter.shouldHide(chat("aaaaaaaa", "Alex", 1)));
        assertFalse(TextMatchRules.isSafeRegex("(a+)+"));
        assertFalse(TextMatchRules.isSafeRegex("(a|aa)+$"));
        assertFalse(TextMatchRules.isSafeRegex("(a|a?)+$"));
    }

    @Test
    void chatFilterSeparatesVisiblePresentationFromLocalHiding() {
        ChatFilter filter = enabled(new ChatFilter());
        stringSetting(filter, "keyword_filters").set("urgent");
        booleanSetting(filter, "hide_matches").set(false);
        booleanSetting(filter, "highlight_matches").set(true);
        booleanSetting(filter, "sound_matches").set(true);
        booleanSetting(filter, "hud_notifications").set(true);

        ChatFilter.Decision decision = filter.evaluate(chat("urgent request", "Bob", 1));
        assertTrue(decision.matched());
        assertFalse(decision.hide());
        assertTrue(decision.highlight());
        assertTrue(decision.sound());
        assertTrue(decision.hudNotification());
    }

    private static IncomingChatMessage chat(String text, String sender, long time) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, text, sender, "", false, time);
    }

    private static IncomingChatMessage game(String text, String key, boolean overlay, long time) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.GAME, text, "", key, overlay, time);
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static StringSetting stringSetting(Module module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
