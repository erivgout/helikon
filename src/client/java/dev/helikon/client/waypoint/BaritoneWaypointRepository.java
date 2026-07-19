package dev.helikon.client.waypoint;

import baritone.api.BaritoneAPI;
import baritone.api.cache.IWaypoint;
import baritone.api.cache.IWaypointCollection;
import baritone.api.utils.BetterBlockPos;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes Baritone's per-world/per-dimension collection the single live waypoint
 * store used by Helikon commands and HUD rendering.
 */
public final class BaritoneWaypointRepository implements WaypointRepository {
    private final WaypointLocationProvider locations;
    private final Supplier<IWaypointCollection> collections;
    private long revision;
    private int observedFingerprint;

    public BaritoneWaypointRepository(WaypointLocationProvider locations) {
        this(locations, BaritoneWaypointRepository::currentCollection);
    }

    BaritoneWaypointRepository(WaypointLocationProvider locations, Supplier<IWaypointCollection> collections) {
        this.locations = Objects.requireNonNull(locations, "locations");
        this.collections = Objects.requireNonNull(collections, "collections");
    }

    @Override
    public synchronized List<Waypoint> forContext(WaypointContext context) {
        return snapshotForContext(context).stream()
                .sorted(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparingLong(Waypoint::createdAtEpochMillis))
                .toList();
    }

    @Override
    public synchronized List<Waypoint> snapshotForContext(WaypointContext context) {
        Objects.requireNonNull(context, "context");
        if (!isCurrent(context)) {
            return List.of();
        }
        IWaypointCollection collection = collection();
        if (collection == null) {
            return List.of();
        }
        return collection.getAllWaypoints().stream()
                .map(waypoint -> adapt(waypoint, context))
                .toList();
    }

    @Override
    public synchronized Optional<Waypoint> find(String name, WaypointContext context) {
        String normalized = Waypoint.normalizedName(name);
        return snapshotForContext(context).stream()
                .filter(waypoint -> Waypoint.normalizedName(waypoint.name()).equals(normalized))
                .max(Comparator.comparingLong(Waypoint::createdAtEpochMillis));
    }

    @Override
    public synchronized long revision() {
        IWaypointCollection collection = collection();
        int fingerprint = collection == null ? 0 : collection.getAllWaypoints().stream()
                .mapToInt(Objects::hashCode)
                .sorted()
                .reduce(1, (left, right) -> 31 * left + right);
        if (fingerprint != observedFingerprint) {
            observedFingerprint = fingerprint;
            revision++;
        }
        return revision;
    }

    @Override
    public synchronized boolean addAndSave(String name, int x, int y, int z, WaypointContext context) {
        String validatedName = Waypoint.requireName(name);
        IWaypointCollection collection = requireCurrentCollection(context);
        if (find(validatedName, context).isPresent()) {
            return false;
        }
        collection.addWaypoint(new baritone.api.cache.Waypoint(validatedName, IWaypoint.Tag.USER,
                new BetterBlockPos(x, y, z)));
        revision++;
        return true;
    }

    @Override
    public synchronized boolean removeAndSave(String name, WaypointContext context) {
        IWaypointCollection collection = requireCurrentCollection(context);
        IWaypoint existing = findBaritone(name, collection).orElse(null);
        if (existing == null) {
            return false;
        }
        collection.removeWaypoint(existing);
        revision++;
        return true;
    }

    @Override
    public synchronized boolean renameAndSave(String sourceName, String targetName, WaypointContext context) {
        String validatedTarget = Waypoint.requireName(targetName);
        IWaypointCollection collection = requireCurrentCollection(context);
        IWaypoint source = findBaritone(sourceName, collection).orElse(null);
        if (source == null) {
            return false;
        }
        Optional<IWaypoint> target = findBaritone(validatedTarget, collection);
        if (target.isPresent() && target.orElseThrow() != source) {
            throw new IllegalArgumentException("A waypoint named '" + validatedTarget + "' already exists here");
        }
        collection.removeWaypoint(source);
        collection.addWaypoint(new baritone.api.cache.Waypoint(validatedTarget, source.getTag(),
                source.getLocation(), source.getCreationTimestamp()));
        revision++;
        return true;
    }

    /** Imports legacy Helikon entries for the currently loaded context once, preserving creation times. */
    public synchronized int migrateCurrent(List<Waypoint> legacy) {
        WaypointLocation location = locations.currentLocation().orElse(null);
        if (location == null) {
            return 0;
        }
        IWaypointCollection collection = collection();
        if (collection == null) {
            return 0;
        }
        int migrated = 0;
        for (Waypoint waypoint : legacy) {
            if (!waypoint.context().equals(location.context())
                    || findBaritone(waypoint.name(), collection).isPresent()) {
                continue;
            }
            collection.addWaypoint(new baritone.api.cache.Waypoint(waypoint.name(), IWaypoint.Tag.USER,
                    new BetterBlockPos(waypoint.x(), waypoint.y(), waypoint.z()), waypoint.createdAtEpochMillis()));
            migrated++;
        }
        if (migrated > 0) {
            revision++;
        }
        return migrated;
    }

    /** Whether Baritone has opened its persistent waypoint collection for the loaded world. */
    public synchronized boolean isReadyForCurrentWorld() {
        return locations.currentLocation().isPresent() && collection() != null;
    }

    private boolean isCurrent(WaypointContext context) {
        return locations.currentLocation().map(location -> location.context().equals(context)).orElse(false);
    }

    private IWaypointCollection requireCurrentCollection(WaypointContext context) {
        if (!isCurrent(context)) {
            throw new IllegalStateException("Baritone waypoints are available only for the loaded world and dimension");
        }
        IWaypointCollection collection = collection();
        if (collection == null) {
            throw new IllegalStateException("Baritone world data is not ready");
        }
        return collection;
    }

    private IWaypointCollection collection() {
        return collections.get();
    }

    private static IWaypointCollection currentCollection() {
        var world = BaritoneAPI.getProvider().getPrimaryBaritone().getWorldProvider().getCurrentWorld();
        return world == null ? null : world.getWaypoints();
    }

    private static Optional<IWaypoint> findBaritone(String name, IWaypointCollection collection) {
        String normalized = Waypoint.normalizedName(name);
        return collection.getAllWaypoints().stream()
                .filter(waypoint -> waypoint.getName().trim().toLowerCase(Locale.ROOT).equals(normalized))
                .max(Comparator.comparingLong(IWaypoint::getCreationTimestamp));
    }

    private static Waypoint adapt(IWaypoint source, WaypointContext context) {
        BetterBlockPos location = source.getLocation();
        return new Waypoint(source.getName(), location.x, location.y, location.z, context,
                color(source.getTag()), source.getTag().getName(), true, source.getCreationTimestamp());
    }

    private static int color(IWaypoint.Tag tag) {
        return switch (tag) {
            case HOME -> 0xFF55FF88;
            case DEATH -> 0xFFFF5555;
            case BED -> 0xFFFF77CC;
            case USER -> Waypoint.DEFAULT_COLOR;
        };
    }
}
