package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapTileStoreTest {
    @TempDir
    Path temporaryDirectory;

    private static final WaypointContext CONTEXT =
            new WaypointContext("world:store", "minecraft:overworld");

    @Test
    void asynchronouslyPersistsAndReloadsPublishedRegions() throws Exception {
        Path root = temporaryDirectory.resolve("maps/v1");
        MapTileStore store = new MapTileStore(root, () -> 64);
        store.activate(CONTEXT);
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));
        assertEquals(MapStoreStatus.State.READY, store.status().state());

        int[] pixels = new int[MapChunkSnapshot.PIXEL_COUNT];
        pixels[0] = 0xFF123456;
        assertTrue(store.submit(new MapChunkSnapshot(CONTEXT, -1, 0, pixels)));
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));
        assertEquals(0xFF123456, store.snapshot(CONTEXT, -1, 0).orElseThrow().pixel(240, 0));
        assertTrue(store.closeAndFlush(Duration.ofSeconds(2)));

        MapTileStore reopened = new MapTileStore(root, () -> 64);
        reopened.activate(CONTEXT);
        assertTrue(reopened.awaitIdle(Duration.ofSeconds(2)));
        reopened.request(CONTEXT, -1, 0);
        assertTrue(reopened.awaitIdle(Duration.ofSeconds(2)));
        assertEquals(0xFF123456, reopened.snapshot(CONTEXT, -1, 0).orElseThrow().pixel(240, 0));
        assertTrue(reopened.closeAndFlush(Duration.ofSeconds(2)));
    }

    @Test
    void pausesAtQuotaWithoutDeletingExistingData() throws Exception {
        Path root = temporaryDirectory.resolve("maps/v1");
        Files.createDirectories(root);
        Path existing = root.resolve("existing.bin");
        try (SeekableByteChannel channel = Files.newByteChannel(existing,
                StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            channel.position(64L * 1024L * 1024L);
            channel.write(java.nio.ByteBuffer.wrap(new byte[]{1}));
        }
        AtomicInteger limit = new AtomicInteger(64);
        MapTileStore store = new MapTileStore(root, limit::get);
        store.activate(CONTEXT);
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));

        assertEquals(MapStoreStatus.State.QUOTA_REACHED, store.status().state());
        assertFalse(store.canAcceptUpdate());
        assertTrue(Files.exists(existing));
        limit.set(128);
        store.recheckQuota();
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));
        assertEquals(MapStoreStatus.State.READY, store.status().state());
        store.closeAndFlush(Duration.ofSeconds(2));
    }

    @Test
    void flushesThePreviousContextBeforeSwitching() throws Exception {
        Path root = temporaryDirectory.resolve("maps/v1");
        WaypointContext other = new WaypointContext("world:other", "minecraft:the_nether");
        MapTileStore store = new MapTileStore(root, () -> 64);
        store.activate(CONTEXT);
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));
        int[] pixels = new int[MapChunkSnapshot.PIXEL_COUNT];
        pixels[0] = 0xFFABCDEF;
        assertTrue(store.submit(new MapChunkSnapshot(CONTEXT, 0, 0, pixels)));
        store.activate(other);
        assertTrue(store.awaitIdle(Duration.ofSeconds(2)));
        assertEquals(other, store.activeContext());
        store.closeAndFlush(Duration.ofSeconds(2));

        MapStorageKey firstKey = new MapStorageKey(root, CONTEXT);
        MapRegionCodec codec = new MapRegionCodec();
        assertEquals(0xFFABCDEF, codec.readWithRecovery(firstKey.regionPath(0, 0), 0, 0).pixel(0, 0));
    }
}
