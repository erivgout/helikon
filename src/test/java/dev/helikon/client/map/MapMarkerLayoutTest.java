package dev.helikon.client.map;

import dev.helikon.client.waypoint.Waypoint;
import dev.helikon.client.waypoint.WaypointContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapMarkerLayoutTest {
    private static final WaypointContext CONTEXT =
            new WaypointContext("world:markers", "minecraft:overworld");

    @Test
    void filtersContextAndViewportAndShowsLabelsByZoomOrHover() {
        Waypoint visible = waypoint("Home", 0, 0, CONTEXT);
        Waypoint otherContext = waypoint("Other", 0, 0,
                new WaypointContext("world:other", "minecraft:overworld"));
        Waypoint offscreen = waypoint("Far", 1000, 1000, CONTEXT);
        MapViewport lowZoom = new MapViewport(0, 0, 0.99D);

        List<MapMarkerLayout.Marker> markers = MapMarkerLayout.layout(
                List.of(visible, otherContext, offscreen), CONTEXT, lowZoom, 800, 600, 0, 0);
        assertEquals(1, markers.size());
        assertFalse(markers.getFirst().labelVisible());

        MapViewport.ScreenPoint home = lowZoom.worldToScreen(0.5D, 0.5D, 800, 600);
        markers = MapMarkerLayout.layout(List.of(visible), CONTEXT, lowZoom,
                800, 600, home.x(), home.y());
        assertTrue(markers.getFirst().hovered());
        assertTrue(markers.getFirst().labelVisible());

        MapViewport fullZoom = new MapViewport(0, 0, 1.0D);
        assertTrue(MapMarkerLayout.layout(List.of(visible), CONTEXT, fullZoom,
                800, 600, 0, 0).getFirst().labelVisible());
    }

    @Test
    void capsVisibleMarkers() {
        List<Waypoint> waypoints = new ArrayList<>();
        for (int index = 0; index < 600; index++) {
            waypoints.add(waypoint("W" + index, index % 10, index / 10, CONTEXT));
        }
        assertEquals(MapMarkerLayout.MAXIMUM_MARKERS, MapMarkerLayout.layout(waypoints, CONTEXT,
                new MapViewport(5, 30, 2), 1000, 1000, -100, -100).size());
    }

    private static Waypoint waypoint(String name, int x, int z, WaypointContext context) {
        return new Waypoint(name, x, 64, z, context, Waypoint.DEFAULT_COLOR,
                Waypoint.NO_ICON, true, 1L);
    }
}

