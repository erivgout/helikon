package dev.helikon.client.module.movement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeldPacketQueueTest {
    @Test
    void releasesOnlyEntriesThatHaveAgedPastTheDelay() {
        HeldPacketQueue<String> queue = new HeldPacketQueue<>();
        assertTrue(queue.enqueue(1000L, "a", 10).isEmpty());
        assertTrue(queue.enqueue(1100L, "b", 10).isEmpty());

        // 150 ms after "a" (aged 150) but only 50 ms after "b": only "a" is releasable at 200 ms delay.
        assertEquals(List.of("a"), queue.drainReleasable(1200L, 200L));
        assertEquals(1, queue.size());

        // Once "b" has aged past the delay it releases too.
        assertEquals(List.of("b"), queue.drainReleasable(1400L, 200L));
        assertTrue(queue.isEmpty());
    }

    @Test
    void releasingPreservesFirstInFirstOutOrder() {
        HeldPacketQueue<Integer> queue = new HeldPacketQueue<>();
        queue.enqueue(0L, 1, 10);
        queue.enqueue(1L, 2, 10);
        queue.enqueue(2L, 3, 10);

        assertEquals(List.of(1, 2, 3), queue.drainReleasable(100L, 0L));
    }

    @Test
    void drainStopsAtTheFirstEntryThatIsStillTooYoung() {
        HeldPacketQueue<String> queue = new HeldPacketQueue<>();
        queue.enqueue(0L, "old", 10);
        queue.enqueue(500L, "young", 10);

        // "old" is releasable but the newer "young" is not, so draining stops after "old".
        assertEquals(List.of("old"), queue.drainReleasable(600L, 200L));
        assertEquals(1, queue.size());
    }

    @Test
    void enqueueingPastCapacityForceReleasesTheOldestEntries() {
        HeldPacketQueue<String> queue = new HeldPacketQueue<>();
        assertTrue(queue.enqueue(0L, "a", 2).isEmpty());
        assertTrue(queue.enqueue(1L, "b", 2).isEmpty());

        // Third entry exceeds the cap of 2, so the oldest ("a") is returned for immediate release.
        assertEquals(List.of("a"), queue.enqueue(2L, "c", 2));
        assertEquals(2, queue.size());
        assertEquals(List.of("b", "c"), queue.drainAll());
    }

    @Test
    void drainAllEmptiesTheBufferInOrder() {
        HeldPacketQueue<Integer> queue = new HeldPacketQueue<>();
        queue.enqueue(0L, 1, 10);
        queue.enqueue(1L, 2, 10);

        assertEquals(List.of(1, 2), queue.drainAll());
        assertTrue(queue.isEmpty());
        assertEquals(List.of(), queue.drainAll());
    }

    @Test
    void clearDiscardsHeldEntriesWithoutReturningThem() {
        HeldPacketQueue<Integer> queue = new HeldPacketQueue<>();
        queue.enqueue(0L, 1, 10);
        queue.clear();
        assertTrue(queue.isEmpty());
    }

    @Test
    void rejectsInvalidArguments() {
        HeldPacketQueue<Integer> queue = new HeldPacketQueue<>();
        assertThrows(IllegalArgumentException.class, () -> queue.enqueue(0L, 1, 0));
        assertThrows(NullPointerException.class, () -> queue.enqueue(0L, null, 5));
        queue.enqueue(0L, 1, 5);
        assertThrows(IllegalArgumentException.class, () -> queue.drainReleasable(10L, -1L));
    }
}
