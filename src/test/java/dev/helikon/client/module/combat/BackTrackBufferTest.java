package dev.helikon.client.module.combat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackTrackBufferTest {
    @Test
    void releasesOnlyPayloadsWhoseDeadlineHasElapsed() {
        BackTrackBuffer<String> buffer = new BackTrackBuffer<>(16);
        assertTrue(buffer.enqueue("a", 100L));
        assertTrue(buffer.enqueue("b", 200L));

        assertEquals(List.of(), buffer.drainDue(50L));
        assertEquals(List.of("a"), buffer.drainDue(100L));
        assertEquals(1, buffer.size());
        assertEquals(List.of("b"), buffer.drainDue(250L));
        assertTrue(buffer.isEmpty());
    }

    @Test
    void headGatesReleaseSoInsertionOrderIsNeverBroken() {
        BackTrackBuffer<String> buffer = new BackTrackBuffer<>(16);
        // The second entry has an earlier deadline, but it must not jump ahead of the head.
        buffer.enqueue("first", 500L);
        buffer.enqueue("second", 100L);

        assertEquals(List.of(), buffer.drainDue(100L));
        assertEquals(List.of("first", "second"), buffer.drainDue(500L));
    }

    @Test
    void drainAllEmptiesInInsertionOrder() {
        BackTrackBuffer<Integer> buffer = new BackTrackBuffer<>(16);
        buffer.enqueue(1, 1000L);
        buffer.enqueue(2, 2000L);
        buffer.enqueue(3, 3000L);

        assertEquals(List.of(1, 2, 3), buffer.drainAll());
        assertTrue(buffer.isEmpty());
    }

    @Test
    void refusesNewPayloadsWhenFullInsteadOfGrowingUnbounded() {
        BackTrackBuffer<String> buffer = new BackTrackBuffer<>(2);
        assertTrue(buffer.enqueue("a", 10L));
        assertTrue(buffer.enqueue("b", 20L));
        assertFalse(buffer.enqueue("c", 30L));
        assertEquals(2, buffer.size());
        assertEquals(List.of("a", "b"), buffer.drainAll());
    }

    @Test
    void clearDiscardsEverything() {
        BackTrackBuffer<String> buffer = new BackTrackBuffer<>(4);
        buffer.enqueue("a", 10L);
        buffer.clear();
        assertTrue(buffer.isEmpty());
        assertEquals(List.of(), buffer.drainDue(Long.MAX_VALUE));
    }

    @Test
    void rejectsInvalidConstructionAndNullPayloads() {
        assertThrows(IllegalArgumentException.class, () -> new BackTrackBuffer<>(0));
        BackTrackBuffer<String> buffer = new BackTrackBuffer<>(4);
        assertThrows(NullPointerException.class, () -> buffer.enqueue(null, 10L));
    }
}
