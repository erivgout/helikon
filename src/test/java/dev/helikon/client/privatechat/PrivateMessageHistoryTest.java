package dev.helikon.client.privatechat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrivateMessageHistoryTest {
    @Test
    void limitsEachConversationAndOrdersTabsByRecentActivity() {
        PrivateMessageHistory history = new PrivateMessageHistory();
        history.recordOutgoing("Alice", "one", 2);
        history.recordIncoming("Bob", "hello", 2);
        history.recordIncoming("alice", "two", 2);
        history.recordOutgoing("Alice", "three", 2);

        assertEquals(List.of("Alice", "Bob"), history.tabs().stream()
                .map(PrivateConversation::participant)
                .toList());
        assertEquals(List.of("two", "three"), history.entries("ALICE").stream()
                .map(PrivateConversation.Entry::text)
                .toList());
        assertEquals(PrivateConversation.Direction.INCOMING, history.entries("alice").getFirst().direction());
    }

    @Test
    void validatesConversationInputsWithoutWritingAnything() {
        PrivateMessageHistory history = new PrivateMessageHistory();

        assertThrows(IllegalArgumentException.class, () -> history.recordOutgoing("not-a-player", "hello", 5));
        assertThrows(IllegalArgumentException.class, () -> history.recordIncoming("Alice", "hello", 101));
        assertEquals(List.of(), history.tabs());
    }

    @Test
    void evictsTheLeastRecentTabWhenTheBoundIsReached() {
        PrivateMessageHistory history = new PrivateMessageHistory();
        for (int index = 0; index <= PrivateMessageHistory.MAX_CONVERSATIONS; index++) {
            history.recordOutgoing("P" + index, "hello", 1);
        }

        assertEquals(PrivateMessageHistory.MAX_CONVERSATIONS, history.tabs().size());
        assertEquals(List.of(), history.entries("P0"));
        assertEquals("P100", history.tabs().getFirst().participant());
    }
}
