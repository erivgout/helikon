package dev.helikon.client.friend;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local-only friend list with schema validation and recoverable JSON storage. */
public final class FriendManager {
    private static final Logger LOGGER = Logger.getLogger(FriendManager.class.getName());
    private static final int SCHEMA_VERSION = 1;

    private final Path friendsPath;
    private final Map<String, Friend> friends = new LinkedHashMap<>();

    public FriendManager(Path configurationDirectory) {
        friendsPath = Objects.requireNonNull(configurationDirectory, "configurationDirectory").resolve("friends.json");
    }

    public synchronized List<Friend> list() {
        return friends.values().stream().sorted(Comparator.comparing(Friend::name, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    public synchronized Optional<Friend> find(String name) {
        return Optional.ofNullable(friends.get(normalizeName(name)));
    }

    public synchronized boolean contains(String name) { return find(name).isPresent(); }

    public synchronized boolean add(String name) {
        String key = normalizeName(name);
        if (friends.containsKey(key)) return false;
        friends.put(key, new Friend(name.trim(), Friend.DEFAULT_COLOR));
        return true;
    }

    public synchronized boolean remove(String name) { return friends.remove(normalizeName(name)) != null; }

    /** Adds an absent name or removes an existing one; returns the new presence. */
    public synchronized boolean toggle(String name) {
        if (remove(name)) return false;
        add(name);
        return true;
    }

    public synchronized boolean setColor(String name, int color) {
        String key = normalizeName(name);
        Friend friend = friends.get(key);
        if (friend == null) return false;
        friends.put(key, new Friend(friend.name(), color));
        return true;
    }

    public synchronized LoadResult load() {
        friends.clear();
        if (Files.notExists(friendsPath)) return LoadResult.MISSING;
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(friendsPath, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) throw new IllegalArgumentException("Friends root must be an object");
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION) throw new IllegalArgumentException("Unsupported friends schema");
            JsonElement entries = root.get("friends");
            if (entries == null || !entries.isJsonArray()) throw new IllegalArgumentException("Missing friends array");
            for (JsonElement entry : entries.getAsJsonArray()) applyEntry(entry);
            return LoadResult.LOADED;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read friends", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid friends file; preserving it without use", exception);
            preserveMalformed();
            friends.clear();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject(); root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray entries = new JsonArray();
        for (Friend friend : list()) {
            JsonObject entry = new JsonObject(); entry.addProperty("name", friend.name()); entry.addProperty("color", friend.color()); entries.add(entry);
        }
        root.add("friends", entries);
        try {
            Files.createDirectories(friendsPath.getParent());
            Path temporary = Files.createTempFile(friendsPath.getParent(), "friends-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(friendsPath)) Files.copy(friendsPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            moveAtomically(temporary, friendsPath);
        } catch (IOException exception) { throw new ConfigurationException("Unable to save friends", exception); }
    }

    private void applyEntry(JsonElement entry) {
        if (!entry.isJsonObject()) throw new IllegalArgumentException("Invalid friend entry");
        JsonObject object = entry.getAsJsonObject();
        JsonElement name = object.get("name");
        if (name == null || !name.isJsonPrimitive() || !name.getAsJsonPrimitive().isString()) throw new IllegalArgumentException("Invalid friend name");
        String displayName = name.getAsString().trim(); String key = normalizeName(displayName);
        if (friends.containsKey(key)) throw new IllegalArgumentException("Duplicate friend name");
        JsonElement color = object.get("color");
        if (color == null || !color.isJsonPrimitive() || !color.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid friend color");
        friends.put(key, new Friend(displayName, requiredInt(object, "color")));
    }

    private void preserveMalformed() {
        if (Files.notExists(friendsPath)) return;
        try { Files.move(friendsPath, friendsPath.resolveSibling("friends.corrupt-" + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.REPLACE_EXISTING); }
        catch (IOException exception) { LOGGER.log(Level.WARNING, "Unable to preserve malformed friends", exception); }
    }

    private Path backupPath() { return friendsPath.resolveSibling("friends.json.bak"); }
    private static String normalizeName(String name) {
        String display = name == null ? "" : name.trim();
        if (!display.matches("[A-Za-z0-9_]{3,16}")) throw new IllegalArgumentException("Friend names must be valid Minecraft player names");
        return display.toLowerCase(Locale.ROOT);
    }
    private static int requiredInt(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw new IllegalArgumentException("Invalid '" + property + "'");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < Integer.MIN_VALUE || number > Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid '" + property + "'");
        return (int) number;
    }
    private static void moveAtomically(Path source, Path destination) throws IOException {
        try { Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
        catch (AtomicMoveNotSupportedException exception) { Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING); }
    }
    public enum LoadResult { MISSING, LOADED, RECOVERED_FROM_ERROR }
}
