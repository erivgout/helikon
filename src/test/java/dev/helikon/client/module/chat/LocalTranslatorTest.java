package dev.helikon.client.module.chat;

import dev.helikon.client.chat.IncomingChatMessage;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalTranslatorTest {
    @Test
    void remainsOfflineAndTranslatesOnlyEnabledVisibleIncomingMessages() {
        LocalTranslator translator = new LocalTranslator();
        IncomingChatMessage message = new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, "hello", "Alice_1", "", false, 1L);
        assertTrue(translator.translate(message).isEmpty());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(translator);
        registry.setEnabled(translator, true);
        stringSetting(translator, "glossary").set("hello=Bonjour");
        assertEquals("Bonjour", translator.translate(message).orElseThrow());
        assertTrue(translator.translate(new IncomingChatMessage(IncomingChatMessage.Channel.GAME, "hello", "", "", true, 1L)).isEmpty());
        assertTrue(translator.translate(new IncomingChatMessage(IncomingChatMessage.Channel.CHAT,
                "x".repeat(257), "", "", false, 1L)).isEmpty());
    }

    private static StringSetting stringSetting(LocalTranslator translator, String id) {
        return (StringSetting) translator.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
