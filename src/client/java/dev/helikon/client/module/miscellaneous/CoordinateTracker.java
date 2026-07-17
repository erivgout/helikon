package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.waypoint.WaypointLocation;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Minecraft-free session memory for the last observed position plus death/logout coordinate snapshots. */
public final class CoordinateTracker {
    private final Map<CoordinateKind, CoordinateEntry> entries = new EnumMap<>(CoordinateKind.class);
    private WaypointLocation lastObservedLocation;

    /** Updates the last usable local position without touching files or network state. */
    public void observe(WaypointLocation location) {
        lastObservedLocation = Objects.requireNonNull(location, "location");
    }

    /** Captures the last observed local position for the requested lifecycle kind. */
    public Optional<CoordinateEntry> record(CoordinateKind kind) {
        CoordinateKind currentKind = Objects.requireNonNull(kind, "kind");
        if (lastObservedLocation == null) {
            return Optional.empty();
        }
        CoordinateEntry entry = new CoordinateEntry(currentKind, lastObservedLocation);
        entries.put(currentKind, entry);
        return Optional.of(entry);
    }

    public Optional<CoordinateEntry> latest(CoordinateKind kind) {
        return Optional.ofNullable(entries.get(Objects.requireNonNull(kind, "kind")));
    }

    /** Returns a snapshot only when it belongs to the current local server or singleplayer-world scope. */
    public Optional<CoordinateEntry> latestForScope(CoordinateKind kind, String scope) {
        String currentScope = dev.helikon.client.waypoint.WaypointContext.requireScope(scope);
        return latest(kind).filter(entry -> entry.location().context().scope().equals(currentScope));
    }

    /** Clears only a stale world-location baseline; saved session entries remain visible. */
    public void clearObservedLocation() {
        lastObservedLocation = null;
    }
}
