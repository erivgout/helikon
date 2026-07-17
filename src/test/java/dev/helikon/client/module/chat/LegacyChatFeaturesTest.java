package dev.helikon.client.module.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyChatFeaturesTest {
    @Test
    void fancyChatStylesOrdinaryTextButPreservesCommands() {
        FancyChat fancy = enabled(new FancyChat());
        assertEquals("ｈｅｌｌｏ", fancy.format("hello"));
        assertEquals("/msg Alex hello", fancy.format("/msg Alex hello"));
        @SuppressWarnings("unchecked")
        EnumSetting<FancyChat.Style> style = (EnumSetting<FancyChat.Style>) fancy.settings().stream()
                .filter(setting -> setting.id().equals("style")).findFirst().orElseThrow();
        style.set(FancyChat.Style.ALTERNATING);
        assertEquals("HeLlO", fancy.format("hello"));
    }

    @Test
    void infiniChatProducesOnlyBoundedCompleteParts() {
        InfiniChat infini = enabled(new InfiniChat());
        String message = ("one two three four five six seven eight nine ten ").repeat(12);
        List<String> parts = infini.split(message);
        assertTrue(parts.size() >= 2);
        assertTrue(parts.stream().allMatch(part -> part.length() <= InfiniChat.CHAT_LIMIT));
        assertTrue(infini.split("/say " + message).isEmpty());
        assertTrue(infini.split("short").isEmpty());
    }

    private static <T extends dev.helikon.client.module.Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
