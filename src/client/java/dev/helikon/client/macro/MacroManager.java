package dev.helikon.client.macro;

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
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Local macro definitions with schema validation, backup, recovery, and transactional edits. */
public final class MacroManager {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_MACROS = 128;
    private static final Logger LOGGER = Logger.getLogger(MacroManager.class.getName());

    private final Path macrosPath;
    private final Map<String, Macro> macros = new LinkedHashMap<>();

    public MacroManager(Path configurationDirectory) {
        macrosPath = Objects.requireNonNull(configurationDirectory, "configurationDirectory").resolve("macros.json");
    }

    public synchronized List<Macro> list() {
        return macros.values().stream()
                .sorted(Comparator.comparing(Macro::name))
                .toList();
    }

    public synchronized Optional<Macro> find(String name) {
        return Optional.ofNullable(macros.get(Macro.normalizeName(name)));
    }

    public synchronized boolean createAndSave(String name, String serverAddress) {
        return persistAfterChange(() -> create(name, serverAddress), Boolean.TRUE::equals);
    }

    public synchronized boolean removeAndSave(String name) {
        return persistAfterChange(() -> remove(name), Boolean.TRUE::equals);
    }

    public synchronized boolean addActionAndSave(String name, MacroAction action) {
        return persistAfterChange(() -> addAction(name, action), Boolean.TRUE::equals);
    }

    public synchronized boolean clearActionsAndSave(String name) {
        return persistAfterChange(() -> clearActions(name), Boolean.TRUE::equals);
    }

    public synchronized boolean setServerAddressAndSave(String name, String serverAddress) {
        return persistAfterChange(() -> setServerAddress(name, serverAddress), Boolean.TRUE::equals);
    }

    public synchronized LoadResult load() {
        macros.clear();
        if (Files.notExists(macrosPath)) {
            return LoadResult.MISSING;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(macrosPath, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Macros root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported macro schema");
            }
            JsonElement entries = root.get("macros");
            if (entries == null || !entries.isJsonArray()) {
                throw new IllegalArgumentException("Missing macros array");
            }
            for (JsonElement entry : entries.getAsJsonArray()) {
                applyEntry(entry);
            }
            return LoadResult.LOADED;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read macros", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid macros file; preserving it without use", exception);
            preserveMalformed();
            macros.clear();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        JsonArray entries = new JsonArray();
        for (Macro macro : list()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("name", macro.name());
            if (macro.isServerScoped()) {
                entry.addProperty("server", macro.serverAddress());
            }
            JsonArray actions = new JsonArray();
            for (MacroAction action : macro.actions()) {
                JsonObject actionObject = new JsonObject();
                actionObject.addProperty("type", action.type().name().toLowerCase(java.util.Locale.ROOT));
                actionObject.addProperty("text", action.text());
                actionObject.addProperty("ticks", action.delayTicks());
                actions.add(actionObject);
            }
            entry.add("actions", actions);
            entries.add(entry);
        }
        root.add("macros", entries);

        try {
            Files.createDirectories(macrosPath.getParent());
            Path temporary = Files.createTempFile(macrosPath.getParent(), "macros-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(macrosPath)) {
                Files.copy(macrosPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, macrosPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save macros", exception);
        }
    }

    private boolean create(String name, String serverAddress) {
        Macro macro = new Macro(name, serverAddress, List.of());
        if (macros.containsKey(macro.name())) {
            return false;
        }
        if (macros.size() >= MAX_MACROS) {
            throw new IllegalStateException("Macro limit of " + MAX_MACROS + " reached");
        }
        macros.put(macro.name(), macro);
        return true;
    }

    private boolean remove(String name) {
        return macros.remove(Macro.normalizeName(name)) != null;
    }

    private boolean addAction(String name, MacroAction action) {
        String key = Macro.normalizeName(name);
        Macro macro = macros.get(key);
        if (macro == null) {
            return false;
        }
        List<MacroAction> actions = new java.util.ArrayList<>(macro.actions());
        actions.add(Objects.requireNonNull(action, "action"));
        macros.put(key, new Macro(macro.name(), macro.serverAddress(), actions));
        return true;
    }

    private boolean clearActions(String name) {
        String key = Macro.normalizeName(name);
        Macro macro = macros.get(key);
        if (macro == null) {
            return false;
        }
        macros.put(key, new Macro(macro.name(), macro.serverAddress(), List.of()));
        return true;
    }

    private boolean setServerAddress(String name, String serverAddress) {
        String key = Macro.normalizeName(name);
        Macro macro = macros.get(key);
        if (macro == null) {
            return false;
        }
        macros.put(key, new Macro(macro.name(), serverAddress, macro.actions()));
        return true;
    }

    private void applyEntry(JsonElement entry) {
        if (!entry.isJsonObject()) {
            throw new IllegalArgumentException("Invalid macro entry");
        }
        JsonObject object = entry.getAsJsonObject();
        String name = requiredString(object, "name");
        String server = optionalString(object, "server", Macro.GLOBAL);
        JsonElement actionEntries = object.get("actions");
        if (actionEntries == null || !actionEntries.isJsonArray()) {
            throw new IllegalArgumentException("Missing macro actions");
        }
        List<MacroAction> actions = actionEntries.getAsJsonArray().asList().stream()
                .map(this::parseAction)
                .toList();
        Macro macro = new Macro(name, server, actions);
        if (macros.putIfAbsent(macro.name(), macro) != null) {
            throw new IllegalArgumentException("Duplicate macro name");
        }
        if (macros.size() > MAX_MACROS) {
            throw new IllegalArgumentException("Macro limit exceeded");
        }
    }

    private MacroAction parseAction(JsonElement entry) {
        if (!entry.isJsonObject()) {
            throw new IllegalArgumentException("Invalid macro action");
        }
        JsonObject object = entry.getAsJsonObject();
        return new MacroAction(
                MacroActionType.parse(requiredString(object, "type")),
                optionalString(object, "text", ""),
                requiredInt(object, "ticks")
        );
    }

    private <T> T persistAfterChange(Supplier<T> mutation, Predicate<T> changed) {
        Map<String, Macro> previous = new LinkedHashMap<>(macros);
        T result = mutation.get();
        if (!changed.test(result)) {
            return result;
        }
        try {
            save();
            return result;
        } catch (RuntimeException exception) {
            macros.clear();
            macros.putAll(previous);
            throw exception;
        }
    }

    private void preserveMalformed() {
        if (Files.notExists(macrosPath)) {
            return;
        }
        try {
            Files.move(macrosPath, macrosPath.resolveSibling("macros.corrupt-"
                    + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed macros", exception);
        }
    }

    private Path backupPath() {
        return macrosPath.resolveSibling("macros.json.bak");
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
