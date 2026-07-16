package dev.helikon.client.waypoint;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.config.ConfigurationException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local, schema-versioned waypoint store with atomic writes and recovery. */
public final class WaypointManager {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_WAYPOINTS = 512;
    private static final Logger LOGGER = Logger.getLogger(WaypointManager.class.getName());

    private final Path waypointsPath;
    private final LongSupplier clock;
    private final Map<String, Waypoint> waypoints = new LinkedHashMap<>();
    private long revision;

    public WaypointManager(Path configurationDirectory) {
        this(configurationDirectory, System::currentTimeMillis);
    }

    WaypointManager(Path configurationDirectory, LongSupplier clock) {
        waypointsPath = Objects.requireNonNull(configurationDirectory, "configurationDirectory").resolve("waypoints.json");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public synchronized List<Waypoint> list() {
        return waypoints.values().stream()
                .sorted(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(waypoint -> waypoint.context().scope())
                        .thenComparing(waypoint -> waypoint.context().dimension()))
                .toList();
    }

    /** Returns all waypoints in one world/server and dimension, including disabled entries. */
    public synchronized List<Waypoint> forContext(WaypointContext context) {
        Objects.requireNonNull(context, "context");
        return snapshotForContext(context).stream()
                .sorted(Comparator.comparing(Waypoint::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Returns an unsorted in-memory snapshot for a bounded render calculation. */
    public synchronized List<Waypoint> snapshotForContext(WaypointContext context) {
        Objects.requireNonNull(context, "context");
        return waypoints.values().stream()
                .filter(waypoint -> waypoint.context().equals(context))
                .toList();
    }

    /** Returns only enabled waypoints visible in the current local context. */
    public synchronized List<Waypoint> visible(WaypointContext context) {
        return snapshotForContext(context).stream().filter(Waypoint::enabled).toList();
    }

    public synchronized Optional<Waypoint> find(String name, WaypointContext context) {
        return Optional.ofNullable(waypoints.get(key(name, context)));
    }

    /** Changes whenever in-memory waypoint data changes, for render-cache invalidation. */
    public synchronized long revision() {
        return revision;
    }

    public synchronized boolean add(String name, int x, int y, int z, WaypointContext context) {
        Waypoint waypoint = new Waypoint(name, x, y, z, context, Waypoint.DEFAULT_COLOR,
                Waypoint.NO_ICON, true, clock.getAsLong());
        String key = key(waypoint.name(), context);
        if (waypoints.containsKey(key)) {
            return false;
        }
        if (waypoints.size() >= MAX_WAYPOINTS) {
            throw new IllegalStateException("Waypoint limit of " + MAX_WAYPOINTS + " reached");
        }
        waypoints.put(key, waypoint);
        revision++;
        return true;
    }

    public synchronized boolean remove(String name, WaypointContext context) {
        boolean removed = waypoints.remove(key(name, context)) != null;
        if (removed) {
            revision++;
        }
        return removed;
    }

    public synchronized boolean rename(String sourceName, String targetName, WaypointContext context) {
        String sourceKey = key(sourceName, context);
        Waypoint source = waypoints.get(sourceKey);
        if (source == null) {
            return false;
        }
        String targetKey = key(targetName, context);
        if (!sourceKey.equals(targetKey) && waypoints.containsKey(targetKey)) {
            throw new IllegalArgumentException("A waypoint named '" + Waypoint.requireName(targetName) + "' already exists here");
        }
        Waypoint renamed = new Waypoint(targetName, source.x(), source.y(), source.z(), source.context(),
                source.color(), source.icon(), source.enabled(), source.createdAtEpochMillis());
        waypoints.remove(sourceKey);
        waypoints.put(targetKey, renamed);
        revision++;
        return true;
    }

    /** Returns the new enabled state, or empty when the waypoint is absent. */
    public synchronized Optional<Boolean> toggle(String name, WaypointContext context) {
        Waypoint waypoint = waypoints.get(key(name, context));
        if (waypoint == null) {
            return Optional.empty();
        }
        boolean enabled = !waypoint.enabled();
        waypoints.put(key(name, context), copy(waypoint, waypoint.name(), waypoint.color(), waypoint.icon(), enabled));
        revision++;
        return Optional.of(enabled);
    }

    public synchronized boolean setColor(String name, WaypointContext context, int color) {
        String key = key(name, context);
        Waypoint waypoint = waypoints.get(key);
        if (waypoint == null) {
            return false;
        }
        waypoints.put(key, copy(waypoint, waypoint.name(), color, waypoint.icon(), waypoint.enabled()));
        revision++;
        return true;
    }

    public synchronized boolean setIcon(String name, WaypointContext context, String icon) {
        String key = key(name, context);
        Waypoint waypoint = waypoints.get(key);
        if (waypoint == null) {
            return false;
        }
        waypoints.put(key, copy(waypoint, waypoint.name(), waypoint.color(), icon, waypoint.enabled()));
        revision++;
        return true;
    }

    /** Applies an add only when its atomic local save can succeed. */
    public synchronized boolean addAndSave(String name, int x, int y, int z, WaypointContext context) {
        return persistAfterChange(() -> add(name, x, y, z, context), Boolean.TRUE::equals);
    }

    /** Applies a removal only when its atomic local save can succeed. */
    public synchronized boolean removeAndSave(String name, WaypointContext context) {
        return persistAfterChange(() -> remove(name, context), Boolean.TRUE::equals);
    }

    /** Applies a rename only when its atomic local save can succeed. */
    public synchronized boolean renameAndSave(String sourceName, String targetName, WaypointContext context) {
        return persistAfterChange(() -> rename(sourceName, targetName, context), Boolean.TRUE::equals);
    }

    /** Applies a visibility change only when its atomic local save can succeed. */
    public synchronized Optional<Boolean> toggleAndSave(String name, WaypointContext context) {
        return persistAfterChange(() -> toggle(name, context), Optional::isPresent);
    }

    /** Applies a color change only when its atomic local save can succeed. */
    public synchronized boolean setColorAndSave(String name, WaypointContext context, int color) {
        return persistAfterChange(() -> setColor(name, context, color), Boolean.TRUE::equals);
    }

    /** Applies an icon change only when its atomic local save can succeed. */
    public synchronized boolean setIconAndSave(String name, WaypointContext context, String icon) {
        return persistAfterChange(() -> setIcon(name, context, icon), Boolean.TRUE::equals);
    }

    public synchronized LoadResult load() {
        waypoints.clear();
        revision++;
        if (Files.notExists(waypointsPath)) {
            return LoadResult.MISSING;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(waypointsPath, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Waypoints root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported waypoint schema");
            }
            JsonElement entries = root.get("waypoints");
            if (entries == null || !entries.isJsonArray()) {
                throw new IllegalArgumentException("Missing waypoints array");
            }
            for (JsonElement entry : entries.getAsJsonArray()) {
                applyEntry(entry);
            }
            return LoadResult.LOADED;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read waypoints", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid waypoints file; preserving it without use", exception);
            preserveMalformed();
            waypoints.clear();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray entries = new JsonArray();
        for (Waypoint waypoint : list()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", waypoint.name());
            entry.addProperty("x", waypoint.x());
            entry.addProperty("y", waypoint.y());
            entry.addProperty("z", waypoint.z());
            entry.addProperty("scope", waypoint.context().scope());
            entry.addProperty("dimension", waypoint.context().dimension());
            entry.addProperty("color", waypoint.color());
            if (!waypoint.icon().isEmpty()) {
                entry.addProperty("icon", waypoint.icon());
            }
            entry.addProperty("enabled", waypoint.enabled());
            entry.addProperty("createdAt", waypoint.createdAtEpochMillis());
            entries.add(entry);
        }
        root.add("waypoints", entries);

        try {
            Files.createDirectories(waypointsPath.getParent());
            Path temporary = Files.createTempFile(waypointsPath.getParent(), "waypoints-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(waypointsPath)) {
                Files.copy(waypointsPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, waypointsPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save waypoints", exception);
        }
    }

    private void applyEntry(JsonElement entry) {
        if (!entry.isJsonObject()) {
            throw new IllegalArgumentException("Invalid waypoint entry");
        }
        JsonObject object = entry.getAsJsonObject();
        WaypointContext context = new WaypointContext(requiredString(object, "scope"), requiredString(object, "dimension"));
        Waypoint waypoint = new Waypoint(
                requiredString(object, "name"),
                requiredInt(object, "x"),
                requiredInt(object, "y"),
                requiredInt(object, "z"),
                context,
                requiredInt(object, "color"),
                optionalString(object, "icon", Waypoint.NO_ICON),
                requiredBoolean(object, "enabled"),
                requiredNonNegativeLong(object, "createdAt")
        );
        String key = key(waypoint.name(), context);
        if (waypoints.putIfAbsent(key, waypoint) != null) {
            throw new IllegalArgumentException("Duplicate waypoint name in one context");
        }
        if (waypoints.size() > MAX_WAYPOINTS) {
            throw new IllegalArgumentException("Waypoint limit exceeded");
        }
    }

    private static Waypoint copy(Waypoint source, String name, int color, String icon, boolean enabled) {
        return new Waypoint(name, source.x(), source.y(), source.z(), source.context(), color, icon, enabled,
                source.createdAtEpochMillis());
    }

    private <T> T persistAfterChange(Supplier<T> mutation, Predicate<T> changed) {
        Map<String, Waypoint> previous = new LinkedHashMap<>(waypoints);
        T result = mutation.get();
        if (!changed.test(result)) {
            return result;
        }
        try {
            save();
            return result;
        } catch (RuntimeException exception) {
            waypoints.clear();
            waypoints.putAll(previous);
            revision++;
            throw exception;
        }
    }

    private void preserveMalformed() {
        if (Files.notExists(waypointsPath)) {
            return;
        }
        try {
            Files.move(waypointsPath, waypointsPath.resolveSibling("waypoints.corrupt-"
                    + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed waypoints", exception);
        }
    }

    private Path backupPath() {
        return waypointsPath.resolveSibling("waypoints.json.bak");
    }

    private static String key(String name, WaypointContext context) {
        return context.scope() + '\u0000' + context.dimension() + '\u0000' + Waypoint.normalizedName(name);
    }

    private static String requiredString(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return value.getAsString();
    }

    private static String optionalString(JsonObject object, String property, String defaultValue) {
        if (!object.has(property)) {
            return defaultValue;
        }
        return requiredString(object, property);
    }

    private static boolean requiredBoolean(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isBoolean()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return value.getAsBoolean();
    }

    private static int requiredInt(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return (int) number;
    }

    private static long requiredNonNegativeLong(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < 0 || number > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return (long) number;
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public enum LoadResult {
        MISSING,
        LOADED,
        RECOVERED_FROM_ERROR
    }
}
