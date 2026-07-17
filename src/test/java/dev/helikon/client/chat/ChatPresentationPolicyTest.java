package dev.helikon.client.chat;

import dev.helikon.client.module.chat.ChatFilter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPresentationPolicyTest {
    @Test
    void suppressesFilterAttentionWhenALaterLocalPolicyHidesTheLine() {
        ChatFilter.Decision matching = new ChatFilter.Decision(true, false, true, true, true);

        ChatFilter.Decision hidden = ChatPresentationPolicy.filterEffectsForVisibleLine(matching, false);
        assertFalse(hidden.matched());
        assertFalse(hidden.highlight());
        assertFalse(hidden.sound());
        assertFalse(hidden.hudNotification());

        ChatFilter.Decision visible = ChatPresentationPolicy.filterEffectsForVisibleLine(matching, true);
        assertTrue(visible.highlight());
        assertTrue(visible.sound());
        assertTrue(visible.hudNotification());
    }
}
