package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRegionTest {
    private static final WaypointContext CONTEXT =
            new WaypointContext("world:map-test", "minecraft:overworld");

    @Test
    void usesFloorDivisionAcrossNegativeRegionBoundaries() {
        assertEquals(-2, MapRegion.regionCoordinateForBlock(-257));
        assertEquals(-1, MapRegion.regionCoordinateForBlock(-256));
        assertEquals(-1, MapRegion.regionCoordinateForBlock(-1));
        assertEquals(0, MapRegion.regionCoordinateForBlock(0));
        assertEquals(0, MapRegion.regionCoordinateForBlock(255));
        assertEquals(1, MapRegion.regionCoordinateForBlock(256));
        assertEquals(255, MapRegion.localCoordinateForBlock(-1));
        assertEquals(0, MapRegion.localCoordinateForBlock(-256));
    }

    @Test
    void appliesOpaqueChunkPixelsWithoutErasingEarlierDiscoveries() {
        int[] firstPixels = new int[MapChunkSnapshot.PIXEL_COUNT];
        firstPixels[0] = 0xFF112233;
        MapRegion region = new MapRegion(-1, -1);
        assertTrue(region.apply(new MapChunkSnapshot(CONTEXT, -1, -1, firstPixels)));
        assertEquals(0xFF112233, region.snapshot().pixel(240, 240));
        assertEquals(1L, region.revision());

        assertFalse(region.apply(new MapChunkSnapshot(CONTEXT, -1, -1,
                new int[MapChunkSnapshot.PIXEL_COUNT])));
        assertEquals(0xFF112233, region.snapshot().pixel(240, 240));
        assertEquals(1L, region.revision());
    }

    @Test
    void rejectsChunksFromAnotherRegion() {
        MapRegion region = new MapRegion(0, 0);
        assertThrows(IllegalArgumentException.class, () -> region.apply(new MapChunkSnapshot(
                CONTEXT, 16, 0, opaquePixels(0xFF334455))));
    }

    private static int[] opaquePixels(int color) {
        int[] pixels = new int[MapChunkSnapshot.PIXEL_COUNT];
        java.util.Arrays.fill(pixels, color);
        return pixels;
    }
}

