package dev.helikon.client.map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapViewportTest {
    @Test
    void roundTripsWorldAndScreenCoordinates() {
        MapViewport viewport = new MapViewport(-100.5D, 72.25D, 2.0D);
        MapViewport.ScreenPoint screen = viewport.worldToScreen(-90.5D, 62.25D, 800, 600);
        MapViewport.WorldPoint world = viewport.screenToWorld(screen.x(), screen.y(), 800, 600);

        assertEquals(-90.5D, world.x(), 0.0001D);
        assertEquals(62.25D, world.z(), 0.0001D);
    }

    @Test
    void keepsCursorWorldPointFixedWhileZoomingAndClampsZoom() {
        MapViewport viewport = new MapViewport(0.0D, 0.0D, 1.0D);
        MapViewport.WorldPoint before = viewport.screenToWorld(700, 100, 800, 600);
        viewport.zoomAt(4.0D, 700, 100, 800, 600);
        MapViewport.WorldPoint after = viewport.screenToWorld(700, 100, 800, 600);

        assertEquals(before.x(), after.x(), 0.0001D);
        assertEquals(before.z(), after.z(), 0.0001D);
        viewport.zoomAt(100.0D, 400, 300, 800, 600);
        assertEquals(MapViewport.MAXIMUM_PIXELS_PER_BLOCK, viewport.pixelsPerBlock());
    }

    @Test
    void pansWithDragAndCapsVisibleRegionRequests() {
        MapViewport viewport = new MapViewport(0.0D, 0.0D, 0.25D);
        viewport.panPixels(25.0D, -50.0D);
        assertEquals(-100.0D, viewport.centerX(), 0.0001D);
        assertEquals(200.0D, viewport.centerZ(), 0.0001D);
        assertTrue(viewport.visibleRegions(100_000, 100_000).size()
                <= MapViewport.MAXIMUM_VISIBLE_REGIONS);
    }
}

