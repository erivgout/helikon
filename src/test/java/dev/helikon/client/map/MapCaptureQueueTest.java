package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCaptureQueueTest {
    private static final WaypointContext CONTEXT =
            new WaypointContext("server:test.example", "minecraft:overworld");

    @Test
    void deduplicatesAndPreservesInsertionOrder() {
        MapCaptureQueue queue = new MapCaptureQueue();
        assertTrue(queue.offer(CONTEXT, 2, 3));
        assertFalse(queue.offer(CONTEXT, 2, 3));
        assertTrue(queue.offer(CONTEXT, -1, 8));
        MapCaptureQueue.Entry first = queue.peek().orElseThrow();
        assertEquals(new MapCaptureQueue.Entry(CONTEXT, 2, 3), first);
        assertTrue(queue.remove(first));
        assertEquals(-1, queue.peek().orElseThrow().chunkX());
    }

    @Test
    void enforcesThePendingChunkBoundAndClears() {
        MapCaptureQueue queue = new MapCaptureQueue();
        for (int index = 0; index < MapCaptureQueue.MAXIMUM_PENDING_CHUNKS; index++) {
            assertTrue(queue.offer(CONTEXT, index, 0));
        }
        assertFalse(queue.offer(CONTEXT, 9999, 0));
        queue.clear();
        assertEquals(0, queue.size());
    }

    @Test
    void prioritizesThePlayerChunkEvenWhenTheQueueIsFull() {
        MapCaptureQueue queue = new MapCaptureQueue();
        for (int index = 0; index < MapCaptureQueue.MAXIMUM_PENDING_CHUNKS; index++) {
            assertTrue(queue.offer(CONTEXT, index, 0));
        }

        queue.prioritize(CONTEXT, 9999, -2);

        assertEquals(MapCaptureQueue.MAXIMUM_PENDING_CHUNKS, queue.size());
        assertEquals(new MapCaptureQueue.Entry(CONTEXT, 9999, -2), queue.peek().orElseThrow());
    }

    @Test
    void movesAnExistingEntryToTheFrontWithoutGrowingTheQueue() {
        MapCaptureQueue queue = new MapCaptureQueue();
        assertTrue(queue.offer(CONTEXT, 1, 0));
        assertTrue(queue.offer(CONTEXT, 2, 0));
        assertTrue(queue.offer(CONTEXT, 3, 0));

        queue.prioritize(CONTEXT, 3, 0);

        assertEquals(3, queue.size());
        assertEquals(3, queue.peek().orElseThrow().chunkX());
    }
}
