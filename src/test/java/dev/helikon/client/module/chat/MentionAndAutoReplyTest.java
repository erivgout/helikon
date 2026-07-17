package dev.helikon.client.module.chat;

import com.mojang.authlib.GameProfile;
import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.chat.IncomingMessageAdapter;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.PlayerChatMessage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MentionAndAutoReplyTest {
    @Test
    void mentionsMatchWholeLocalPlayerNamesAndRespectCooldown() {
        MentionNotifier notifier = enabled(new MentionNotifier());
        IncomingChatMessage mention = chat("Bob", "Hey Alice, are you there?", 1_000L);

        assertTrue(notifier.shouldNotify(mention, "Alice"));
        assertFalse(notifier.shouldNotify(chat("Bob", "Alice again", 2_000L), "Alice"));
        assertTrue(notifier.shouldNotify(chat("Bob", "Alice again", 6_000L), "Alice"));
        assertFalse(notifier.shouldNotify(chat("Bob", "Malice is here", 12_000L), "Alice"));
    }

    @Test
    void mentionTermsAreLocalAndIgnoreMessagesFromTheLocalPlayer() {
        MentionNotifier notifier = enabled(new MentionNotifier());
        stringSetting(notifier, "mention_terms").set("urgent,help");

        assertTrue(notifier.shouldNotify(chat("Bob", "Need urgent help", 1_000L), "Alice"));
        assertFalse(notifier.shouldNotify(chat("alice", "urgent", 7_000L), "Alice"));
    }

    @Test
    void autoReplyUsesSafeRuleCooldownServerAndLoopGuards() {
        AutoReply autoReply = enabled(new AutoReply());
        stringSetting(autoReply, "trigger").set("ping");
        stringSetting(autoReply, "reply").set("pong");
        stringSetting(autoReply, "server_restriction").set("example.org");
        stringSetting(autoReply, "whitelist").set("Bob");

        assertEquals(Optional.empty(), autoReply.replyFor(chat("Eve", "ping", 1_000L), "Alice", "example.org", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Bob", "ping", 1_000L), "Alice", "other.org", false));
        assertEquals(Optional.of("pong"), autoReply.replyFor(chat("Bob", "ping", 1_000L), "Alice", "EXAMPLE.ORG", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Bob", "ping", 2_000L), "Alice", "example.org", false));
    }

    @Test
    void autoReplySuppressesAnEchoOfItsRecentReplyText() {
        AutoReply autoReply = enabled(new AutoReply());
        stringSetting(autoReply, "trigger").set("pong");
        stringSetting(autoReply, "reply").set("pong");

        assertEquals(Optional.of("pong"), autoReply.replyFor(chat("Bob", "pong", 1_000L), "Alice", "", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Eve", "pong", 40_000L), "Alice", "", false));
    }

    @Test
    void autoReplyValidatesOutputAndHonorsTheLocalPerMinuteLimit() {
        AutoReply autoReply = enabled(new AutoReply());
        stringSetting(autoReply, "trigger").set("hello");
        stringSetting(autoReply, "reply").set("/not-a-chat-reply");
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Bob", "hello", 1_000L), "Alice", "", false));

        stringSetting(autoReply, "reply").set("hello back");
        numberSetting(autoReply, "cooldown_seconds").set(5.0);
        numberSetting(autoReply, "replies_per_minute").set(1.0);
        assertEquals(Optional.of("hello back"), autoReply.replyFor(chat("Bob", "hello", 10_000L), "Alice", "", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Eve", "hello", 20_000L), "Alice", "", false));
        assertEquals(Optional.of("hello back"), autoReply.replyFor(chat("Eve", "hello", 70_000L), "Alice", "", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Bob", "hello", 80_000L), "Alice", "", true));
    }

    @Test
    void autoReplyUsesTheBoundedRegexMatcher() {
        AutoReply autoReply = enabled(new AutoReply());
        stringSetting(autoReply, "trigger").set("^hello\\s+there$");
        enumSetting(autoReply, "match_mode").set(AutoReply.MatchMode.REGEX);
        stringSetting(autoReply, "reply").set("hi");

        assertEquals(Optional.of("hi"), autoReply.replyFor(chat("Bob", "Hello there", 1_000L), "Alice", "", false));
        assertEquals(Optional.empty(), autoReply.replyFor(chat("Eve", "hello there friend", 2_000L), "Alice", "", false));
    }

    @Test
    void autoReplyMatchesTheRawSignedBodyInsteadOfTheDecoratedChatLine() {
        AutoReply autoReply = enabled(new AutoReply());
        stringSetting(autoReply, "trigger").set("ping");
        enumSetting(autoReply, "match_mode").set(AutoReply.MatchMode.EXACT);
        stringSetting(autoReply, "reply").set("pong");
        IncomingChatMessage decorated = IncomingMessageAdapter.chat(
                Component.literal("<Bob> ping"),
                PlayerChatMessage.unsigned(UUID.randomUUID(), "ping"),
                new GameProfile(UUID.randomUUID(), "Bob"), 1_000L
        );

        assertEquals("<Bob> ping", decorated.text());
        assertEquals("ping", decorated.rawText());
        assertEquals(Optional.of("pong"), autoReply.replyFor(decorated, "Alice", "", false));
    }

    private static IncomingChatMessage chat(String sender, String text, long at) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, text, sender, "", false, at);
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static StringSetting stringSetting(Module module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Module module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<AutoReply.MatchMode> enumSetting(Module module, String id) {
        return (EnumSetting<AutoReply.MatchMode>) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
