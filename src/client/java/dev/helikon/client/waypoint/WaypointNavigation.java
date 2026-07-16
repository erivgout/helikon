package dev.helikon.client.waypoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Minecraft-free context filtering, distance sorting, and compass labels. */
public final class WaypointNavigation {
    private static final String[] COMPASS = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
    private static final Comparator<LocatedWaypoint> DISTANCE_ORDER = Comparator
            .comparingInt(LocatedWaypoint::distance)
            .thenComparing(located -> located.waypoint().name(), String.CASE_INSENSITIVE_ORDER);

    private WaypointNavigation() {
    }

    public static List<LocatedWaypoint> visibleSorted(List<Waypoint> waypoints, WaypointLocation location) {
        Objects.requireNonNull(waypoints, "waypoints");
        Objects.requireNonNull(location, "location");
        return waypoints.stream()
                .filter(Waypoint::enabled)
                .filter(waypoint -> waypoint.context().equals(location.context()))
                .map(waypoint -> locate(waypoint, location))
                .sorted(DISTANCE_ORDER)
                .toList();
    }

    /**
     * Selects at most {@code maximumEntries} nearest visible waypoints without
     * sorting every entry. HUD adapters cache this bounded result.
     */
    public static List<LocatedWaypoint> nearestVisible(
            Iterable<Waypoint> waypoints,
            WaypointLocation location,
            int maximumEntries
    ) {
        Objects.requireNonNull(waypoints, "waypoints");
        Objects.requireNonNull(location, "location");
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("Maximum entries must be positive");
        }

        List<LocatedWaypoint> nearest = new ArrayList<>(maximumEntries);
        for (Waypoint waypoint : waypoints) {
            if (!waypoint.enabled() || !waypoint.context().equals(location.context())) {
                continue;
            }
            LocatedWaypoint candidate = locate(waypoint, location);
            int insertion = 0;
            while (insertion < nearest.size() && DISTANCE_ORDER.compare(nearest.get(insertion), candidate) <= 0) {
                insertion++;
            }
            if (insertion < maximumEntries) {
                nearest.add(insertion, candidate);
                if (nearest.size() > maximumEntries) {
                    nearest.remove(maximumEntries);
                }
            }
        }
        return List.copyOf(nearest);
    }

    public static LocatedWaypoint locate(Waypoint waypoint, WaypointLocation location) {
        Objects.requireNonNull(waypoint, "waypoint");
        Objects.requireNonNull(location, "location");
        if (!waypoint.context().equals(location.context())) {
            throw new IllegalArgumentException("Waypoint belongs to a different local context");
        }
        double dx = waypoint.x() - location.x();
        double dy = waypoint.y() - location.y();
        double dz = waypoint.z() - location.z();
        int distance = (int) Math.min(Integer.MAX_VALUE, Math.round(Math.sqrt(dx * dx + dy * dy + dz * dz)));
        return new LocatedWaypoint(waypoint, distance, direction(dx, dz));
    }

    private static String direction(double dx, double dz) {
        if (dx * dx + dz * dz < 0.25D) {
            return "here";
        }
        double bearing = Math.toDegrees(Math.atan2(dx, -dz));
        int index = Math.floorMod((int) Math.floor((bearing + 22.5D) / 45D), COMPASS.length);
        return COMPASS[index];
    }

    /** A visible waypoint annotated with current distance and global compass direction. */
    public record LocatedWaypoint(Waypoint waypoint, int distance, String direction) {
    }
}
