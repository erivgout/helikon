package dev.helikon.client.module.chat;

import dev.helikon.client.setting.StringSetting;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateMessageHelperTest {
    @Test
    void exposesOnlyOneSafeConfigurableCommandToken() {
        PrivateMessageHelper helper = new PrivateMessageHelper();
        assertEquals("msg", helper.messageCommand().orElseThrow());
        assertEquals("r", helper.replyCommand().orElseThrow());

        stringSetting(helper, "message_command").set("tell-raw_2");
        assertEquals("tell-raw_2", helper.messageCommand().orElseThrow());

        stringSetting(helper, "reply_command").set("r all");
        assertTrue(helper.replyCommand().isEmpty());
    }

    @Test
    void exposesLocalIncomingPresentationPreferences() {
        PrivateMessageHelper helper = new PrivateMessageHelper();
        assertTrue(helper.notifications());
        assertTrue(helper.sound());
        assertTrue(helper.highlight());

        booleanSetting(helper, "sound").set(false);
        booleanSetting(helper, "highlight").set(false);
        assertTrue(helper.notifications());
        assertFalse(helper.sound());
        assertFalse(helper.highlight());
    }

    private static StringSetting stringSetting(PrivateMessageHelper helper, String id) {
        return (StringSetting) helper.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static BooleanSetting booleanSetting(PrivateMessageHelper helper, String id) {
        return (BooleanSetting) helper.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
