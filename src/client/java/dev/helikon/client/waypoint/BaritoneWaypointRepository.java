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
import java.util.logging.Logger;

/**
 * Makes Baritone's per-world/per-dimension collection the single live waypoint
 * store used by Helikon commands and HUD rendering.
 */
public final class BaritoneWaypointRepository implements WaypointRepository {
    private static final Logger LOGGER = Logger.getLogger(BaritoneWaypointRepository.class.getName());

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
                .flatMap(waypoint -> adapt(waypoint, context).stream())
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
                .filter(Objects::nonNull)
                .filter(waypoint -> externalName(waypoint).map(value ->
                        value.toLowerCase(Locale.ROOT).equals(normalized)).orElse(false))
                .max(Comparator.comparingLong(IWaypoint::getCreationTimestamp));
    }

    private static Optional<Waypoint> adapt(IWaypoint source, WaypointContext context) {
        if (source == null) {
            LOGGER.warning("Ignoring null Baritone waypoint entry");
            return Optional.empty();
        }
        try {
            IWaypoint.Tag tag = Objects.requireNonNull(source.getTag(), "Baritone waypoint tag");
            BetterBlockPos location = Objects.requireNonNull(source.getLocation(), "Baritone waypoint location");
            String name = externalName(source).orElseThrow();
            return Optional.of(new Waypoint(name, location.x, location.y, location.z, context,
                    color(tag), tag.getName(), true, source.getCreationTimestamp()));
        } catch (IllegalArgumentException | NullPointerException exception) {
            LOGGER.warning(() -> "Ignoring incompatible Baritone waypoint entry: " + exception.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Baritone permits blank and otherwise unrestricted waypoint names. Helikon
     * keeps its stricter local model, so external names receive a deterministic
     * display-safe form at the repository boundary.
     */
    private static Optional<String> externalName(IWaypoint source) {
        if (source == null || source.getTag() == null) {
            return Optional.empty();
        }
        String fallback = switch (source.getTag()) {
            case HOME -> "Home";
            case DEATH -> "Death";
            case BED -> "Bed";
            case USER -> "Waypoint";
        };
        String input = Objects.requireNonNullElse(source.getName(), "").trim();
        if (input.isEmpty()) {
            return Optional.of(fallback);
        }

        StringBuilder sanitized = new StringBuilder(Math.min(input.length(), 32));
        boolean replacement = false;
        for (int index = 0; index < input.length() && sanitized.length() < 32; index++) {
            char character = input.charAt(index);
            if (isNameCharacter(character)) {
                sanitized.append(character);
                replacement = false;
            } else if (!replacement) {
                sanitized.append('_');
                replacement = true;
            }
        }
        String value = sanitized.toString().trim();
        if (value.isEmpty()) {
            value = fallback;
        } else if (!isAsciiLetterOrDigit(value.charAt(0))) {
            value = (fallback + " " + value);
            if (value.length() > 32) {
                value = value.substring(0, 32).trim();
            }
        }
        return Optional.of(Waypoint.requireName(value));
    }

    private static boolean isNameCharacter(char character) {
        return isAsciiLetterOrDigit(character) || character == ' '
                || character == '_' || character == '-';
    }

    private static boolean isAsciiLetterOrDigit(char character) {
        return character >= 'A' && character <= 'Z'
                || character >= 'a' && character <= 'z'
                || character >= '0' && character <= '9';
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
