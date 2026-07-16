package dev.helikon.client.waypoint;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WaypointNavigationTest {
    private static final WaypointContext CONTEXT = new WaypointContext("world:tests", "minecraft:overworld");
    private static final WaypointContext OTHER_CONTEXT = new WaypointContext("world:other", "minecraft:overworld");

    @Test
    void filtersContextAndDisabledEntriesThenSortsByDistance() {
        WaypointLocation player = new WaypointLocation(0, 64, 0, CONTEXT);
        Waypoint east = waypoint("East", 20, 64, 0, CONTEXT, true);
        Waypoint north = waypoint("North", 0, 64, -10, CONTEXT, true);
        Waypoint disabled = waypoint("Disabled", 1, 64, 0, CONTEXT, false);
        Waypoint unrelated = waypoint("Elsewhere", 1, 64, 0, OTHER_CONTEXT, true);

        List<WaypointNavigation.LocatedWaypoint> located = WaypointNavigation.visibleSorted(
                List.of(east, north, disabled, unrelated), player);

        assertEquals(List.of("North", "East"), located.stream().map(item -> item.waypoint().name()).toList());
        assertEquals("N", located.get(0).direction());
        assertEquals("E", located.get(1).direction());
        assertEquals(10, located.get(0).distance());
    }

    @Test
    void labelsAPositionAtTheWaypointAsHere() {
        WaypointLocation player = new WaypointLocation(3, 70, -4, CONTEXT);

        WaypointNavigation.LocatedWaypoint located = WaypointNavigation.locate(
                waypoint("Here", 3, 70, -4, CONTEXT, true), player);

        assertEquals("here", located.direction());
        assertEquals(0, located.distance());
    }

    @Test
    void retainsOnlyTheRequestedNearestEntriesWithoutSortingTheRest() {
        WaypointLocation player = new WaypointLocation(0, 64, 0, CONTEXT);
        List<WaypointNavigation.LocatedWaypoint> nearest = WaypointNavigation.nearestVisible(List.of(
                waypoint("Far", 100, 64, 0, CONTEXT, true),
                waypoint("Near", 4, 64, 0, CONTEXT, true),
                waypoint("Middle", 20, 64, 0, CONTEXT, true),
                waypoint("Other", 50, 64, 0, CONTEXT, true)
        ), player, 2);

        assertEquals(List.of("Near", "Middle"), nearest.stream().map(item -> item.waypoint().name()).toList());
    }

    private static Waypoint waypoint(String name, int x, int y, int z, WaypointContext context, boolean enabled) {
        return new Waypoint(name, x, y, z, context, Waypoint.DEFAULT_COLOR, Waypoint.NO_ICON, enabled, 1L);
    }
}
