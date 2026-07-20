package dev.helikon.client.waypoint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointMarkerPresentationTest {
    private static final WaypointContext CONTEXT =
            new WaypointContext("world:marker-test", "minecraft:overworld");

    @Test
    void buildsACompactLunarStyleLabelAndInitial() {
        Waypoint waypoint = new Waypoint("Home", 10, 64, -5, CONTEXT,
                0xFF55FF88, "home", true, 1L);

        WaypointMarkerPresentation.Marker marker = WaypointMarkerPresentation.marker(
                new WaypointNavigation.LocatedWaypoint(waypoint, 13, "E"));

        assertEquals("H", marker.icon());
        assertEquals("Home [13m]", marker.label());
    }

    @Test
    void keepsNearbyLabelsCompactAndStronglyCompensatesForDistance() {
        assertEquals(0.85F, WaypointMarkerPresentation.textScale(0), 0.0001F);
        assertEquals(0.85F, WaypointMarkerPresentation.textScale(8), 0.0001F);
        assertEquals(0.856F, WaypointMarkerPresentation.textScale(10), 0.0001F);
        assertEquals(1.066F, WaypointMarkerPresentation.textScale(80), 0.0001F);
        assertTrue(WaypointMarkerPresentation.textScale(250) > WaypointMarkerPresentation.textScale(80));
        assertEquals(1.8F, WaypointMarkerPresentation.textScale(10_000), 0.0001F);
    }

    @Test
    void liftsDistantLabelsOntoAVisiblePartOfTheBeam() {
        assertEquals(82.0D, WaypointMarkerPresentation.labelY(60.0D, 80.0D, 5), 0.0001D);
        assertEquals(90.0D, WaypointMarkerPresentation.labelY(60.0D, 80.0D, 250), 0.0001D);
        assertEquals(122.0D, WaypointMarkerPresentation.labelY(120.0D, 80.0D, 250), 0.0001D);
    }

    @Test
    void usesAnXForDeathWaypoints() {
        Waypoint waypoint = new Waypoint("Death", 0, 64, 0, CONTEXT,
                0xFFFF5555, "death", true, 1L);

        assertEquals("X", WaypointMarkerPresentation.marker(
                new WaypointNavigation.LocatedWaypoint(waypoint, 5, "N")).icon());
    }
}
