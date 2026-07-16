package dev.helikon.client.waypoint;

import java.util.Optional;

/** Minecraft-free boundary for obtaining the current waypoint location. */
@FunctionalInterface
public interface WaypointLocationProvider {
    Optional<WaypointLocation> currentLocation();
}
