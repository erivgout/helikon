package dev.helikon.client.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatPrefix;
import dev.helikon.client.module.chat.ChatSuffix;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OutgoingChatFormatterTest {
    @Test
    void combinesSafePrefixAndServerSpecificSuffix() {
        ChatPrefix prefix = enabled(new ChatPrefix());
        ChatSuffix suffix = enabled(new ChatSuffix());
        stringSetting(prefix, "prefix").set("[H]");
        stringSetting(suffix, "suffix").set("default");
        stringSetting(suffix, "per_server_suffixes").set("example.org=local;other.net=other");
        OutgoingChatFormatter formatter = new OutgoingChatFormatter(prefix, suffix, () -> "EXAMPLE.ORG", () -> 4);

        assertEquals("[H] hello | local", formatter.format("hello"));
    }

    @Test
    void choosesDeterministicRandomSuffixWhenNoServerEntryExists() {
        ChatPrefix prefix = new ChatPrefix();
        ChatSuffix suffix = enabled(new ChatSuffix());
        stringSetting(suffix, "suffix").set("default");
        stringSetting(suffix, "random_suffixes").set("one,two,three");
        OutgoingChatFormatter formatter = new OutgoingChatFormatter(prefix, suffix, () -> "unknown.test", () -> 5);

        assertEquals("hello | three", formatter.format("hello"));
    }

    @Test
    void preservesLocalCommandsAuthenticationAndPrivateMessageInput() {
        ChatPrefix prefix = enabled(new ChatPrefix());
        ChatSuffix suffix = enabled(new ChatSuffix());
        stringSetting(prefix, "prefix").set("[H]");
        stringSetting(suffix, "suffix").set("client");
        OutgoingChatFormatter formatter = new OutgoingChatFormatter(prefix, suffix, () -> null, () -> 0);

        assertEquals(".help", formatter.format(".help"));
        assertEquals("/login secret", formatter.format("/login secret"));
        assertEquals("/password secret", formatter.format("/password secret"));
        assertEquals("/msg Friend private", formatter.format("/msg Friend private"));
        assertEquals("/pm Friend private", formatter.format("/pm Friend private"));
    }

    @Test
    void declinesFormattingThatWouldExceedTheVanillaPacketLimit() {
        ChatPrefix prefix = new ChatPrefix();
        ChatSuffix suffix = enabled(new ChatSuffix());
        stringSetting(suffix, "suffix").set("x");
        OutgoingChatFormatter formatter = new OutgoingChatFormatter(prefix, suffix, () -> null, () -> 0);

        String maximumFormatted = "a".repeat(252);
        String overlongFormatted = "b".repeat(253);
        assertEquals(maximumFormatted + " | x", formatter.format(maximumFormatted));
        assertEquals(overlongFormatted, formatter.format(overlongFormatted));
    }

    private static <T extends dev.helikon.client.module.Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static StringSetting stringSetting(dev.helikon.client.module.Module module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
