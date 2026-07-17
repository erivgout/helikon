package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.waypoint.WaypointLocation;

import java.util.Objects;

/** A session-local coordinate snapshot made from the already-observed current location. */
public record CoordinateEntry(CoordinateKind kind, WaypointLocation location) {
    public CoordinateEntry {
        kind = Objects.requireNonNull(kind, "kind");
        location = Objects.requireNonNull(location, "location");
    }

    /** A compact local display string with exact block coordinates and dimension. */
    public String displayText() {
        return kind.displayName() + ": " + location.x() + ", " + location.y() + ", " + location.z()
                + " (" + location.context().dimension() + ")";
    }
}
