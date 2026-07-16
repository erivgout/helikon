package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.PanicKeybindManager;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.function.IntPredicate;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Persists only the configurable local panic key in recoverable {@code panic.json}. */
public final class PanicConfigurationManager {
    public static final int SCHEMA_VERSION = 1;
    private static final Logger LOGGER = Logger.getLogger(PanicConfigurationManager.class.getName());

    private final Path panicPath;
    private final IntPredicate reservedKeys;

    public PanicConfigurationManager(Path configurationDirectory) {
        this(configurationDirectory, key -> false);
    }

    public PanicConfigurationManager(Path configurationDirectory, IntPredicate reservedKeys) {
        panicPath = Objects.requireNonNull(configurationDirectory, "configurationDirectory").resolve("panic.json");
        this.reservedKeys = Objects.requireNonNull(reservedKeys, "reservedKeys");
    }

    public synchronized LoadResult load(PanicKeybindManager keybind) {
        Objects.requireNonNull(keybind, "keybind");
        keybind.setKeybind(Keybind.unbound());
        if (Files.notExists(panicPath)) {
            return LoadResult.MISSING;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(panicPath, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Panic configuration root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported panic configuration schema");
            }
            keybind.setKeybind(requireAllowed(new Keybind(requiredInt(root, "key"), Keybind.Activation.TOGGLE)));
            return LoadResult.LOADED;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read panic configuration", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid panic configuration; preserving it without use", exception);
            keybind.setKeybind(Keybind.unbound());
            preserveMalformed();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    /** Applies a new key only when the local atomic save succeeds. */
    public synchronized void setKeybindAndSave(PanicKeybindManager keybind, Keybind next) {
        Objects.requireNonNull(keybind, "keybind");
        Keybind previous = keybind.keybind();
        keybind.setKeybind(requireAllowed(Objects.requireNonNull(next, "next")));
        try {
            save(keybind);
        } catch (RuntimeException exception) {
            keybind.setKeybind(previous);
            throw exception;
        }
    }

    public synchronized void save(PanicKeybindManager keybind) {
        Objects.requireNonNull(keybind, "keybind");
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("key", keybind.keybind().keyCode());
        try {
            Files.createDirectories(panicPath.getParent());
            Path temporary = Files.createTempFile(panicPath.getParent(), "panic-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(panicPath)) {
                Files.copy(panicPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, panicPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save panic configuration", exception);
        }
    }

    private void preserveMalformed() {
        if (Files.notExists(panicPath)) {
            return;
        }
        try {
            Files.move(panicPath, panicPath.resolveSibling("panic.corrupt-"
                    + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed panic configuration", exception);
        }
    }

    private Path backupPath() {
        return panicPath.resolveSibling("panic.json.bak");
    }

    private Keybind requireAllowed(Keybind keybind) {
        if (keybind.isBound() && reservedKeys.test(keybind.keyCode())) {
            throw new IllegalArgumentException("The Helikon GUI key is reserved");
        }
        return keybind;
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
