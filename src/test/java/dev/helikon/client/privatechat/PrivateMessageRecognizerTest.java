package dev.helikon.client.privatechat;

import dev.helikon.client.chat.IncomingChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrivateMessageRecognizerTest {
    @Test
    void recognizesOnlyCommonInboundPrivateMessageForms() {
        assertEquals(new PrivateMessageRecognizer.Incoming("Bob", "hello"), recognize("From Bob: hello").orElseThrow());
        assertEquals(new PrivateMessageRecognizer.Incoming("Eve", "secret"),
                recognize("Eve whispers to you: secret").orElseThrow());
        assertEquals(new PrivateMessageRecognizer.Incoming("Kai", "hi"), recognize("Kai -> you: hi").orElseThrow());
    }

    @Test
    void rejectsSelfMessagesGameMessagesAndUnstructuredText() {
        assertTrue(PrivateMessageRecognizer.recognize(message("From Alice: loop"), "Alice").isEmpty());
        assertTrue(PrivateMessageRecognizer.recognize(message("ordinary chat"), "Alice").isEmpty());
        assertTrue(PrivateMessageRecognizer.recognize(new IncomingChatMessage(IncomingChatMessage.Channel.GAME,
                "From Bob: hello", "", "", false, 1L), "Alice").isEmpty());
    }

    private static java.util.Optional<PrivateMessageRecognizer.Incoming> recognize(String text) {
        return PrivateMessageRecognizer.recognize(message(text), "Alice");
    }

    private static IncomingChatMessage message(String text) {
        return new IncomingChatMessage(IncomingChatMessage.Channel.CHAT, text, "", "", false, 1L);
    }
}
