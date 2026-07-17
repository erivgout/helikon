package dev.helikon.client.chat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterChatPolicyTest {
    @Test
    void returnsVanillaValuesWhileTheModuleIsDisabledAndBoundsEnabledValues() {
        assertEquals(100, BetterChatRenderPolicy.historyLimit(false, 2_000.0D));
        assertEquals(2_000, BetterChatRenderPolicy.historyLimit(true, 3_000.0D));
        assertEquals(200, BetterChatRenderPolicy.totalLifetimeTicks(false, 600.0D, 60.0D));
        assertEquals(13_200, BetterChatRenderPolicy.totalLifetimeTicks(true, 1_000.0D, 1_000.0D));
        assertEquals(10.0D, BetterChatRenderPolicy.fadeMultiplier(false, 60.0D, 1.0D));
        assertEquals(11.0D, BetterChatRenderPolicy.fadeMultiplier(true, 10.0D, 1.0D));
        assertEquals(9.0D, BetterChatRenderPolicy.lineHeight(false, true, 9.0D));
        assertEquals(7.0D, BetterChatRenderPolicy.lineHeight(true, true, 9.0D));
    }

    @Test
    void tracksOnlyConsecutiveDuplicatesAndCapsTheirCounter() {
        ChatDuplicateTracker tracker = new ChatDuplicateTracker();
        assertEquals(new ChatDuplicateTracker.Decision(1, false, false), tracker.record("player\u0000hello", true, true));
        assertEquals(new ChatDuplicateTracker.Decision(2, true, true), tracker.record("player\u0000hello", true, true));
        assertEquals(new ChatDuplicateTracker.Decision(1, false, false), tracker.record("player\u0000other", true, true));
        assertEquals(new ChatDuplicateTracker.Decision(2, false, false), tracker.record("player\u0000other", false, false));
        assertEquals(new ChatDuplicateTracker.Decision(3, false, true), tracker.record("player\u0000other", false, true));
    }

    @Test
    void findsBoundedLocalHistoryAndRejectsUnsafeQueries() {
        assertEquals(List.of("Newest HELLO", "hello again"),
                ChatHistorySearch.find(List.of("Newest HELLO", "nothing", "hello again"), " hello ", 8));
        assertThrows(IllegalArgumentException.class, () -> ChatHistorySearch.find(List.of("x"), "\n", 8));
    }

    @Test
    void validatesOnlyStandardVanillaPlayerNamesForLocalClickActions() {
        assertTrue(ChatPlayerNamePolicy.isVanillaPlayerName("Player_1"));
        assertFalse(ChatPlayerNamePolicy.isVanillaPlayerName("not a player"));
        assertFalse(ChatPlayerNamePolicy.isVanillaPlayerName("abcdefghijklmnopq"));
    }

    @Test
    void easesLargeScrollRequestsInBoundedSteps() {
        SmoothScrollState state = new SmoothScrollState();
        state.request(10);

        assertEquals(4, state.nextStep());
        assertEquals(3, state.nextStep());
        assertEquals(2, state.nextStep());
        assertEquals(1, state.nextStep());
        assertEquals(0, state.pendingLines());
        assertEquals(0, state.nextStep());
    }
}
