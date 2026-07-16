package dev.helikon.client.waypoint;

import java.util.Objects;

/** Current player block coordinates and their local waypoint context. */
public record WaypointLocation(int x, int y, int z, WaypointContext context) {
    public WaypointLocation {
        context = Objects.requireNonNull(context, "context");
    }
}
