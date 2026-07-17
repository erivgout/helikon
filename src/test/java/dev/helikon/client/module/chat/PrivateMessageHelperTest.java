package dev.helikon.client.module.chat;

import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static StringSetting stringSetting(PrivateMessageHelper helper, String id) {
        return (StringSetting) helper.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
