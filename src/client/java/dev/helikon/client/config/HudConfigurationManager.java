package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.hud.HudLayout;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the small, schema-versioned HUD layout in {@code hud.json}. It uses
 * the same local-only atomic-write and corrupt-file recovery guarantees as the
 * global module configuration.
 */
public final class HudConfigurationManager {
    public static final int SCHEMA_VERSION = 1;

    private static final Logger LOGGER = Logger.getLogger(HudConfigurationManager.class.getName());

    private final Path configurationDirectory;
    private final Path hudConfigurationPath;

    public HudConfigurationManager(Path configurationDirectory) {
        this.configurationDirectory = Objects.requireNonNull(configurationDirectory, "configurationDirectory").normalize();
        this.hudConfigurationPath = this.configurationDirectory.resolve("hud.json");
    }

    public Path hudConfigurationPath() {
        return hudConfigurationPath;
    }

    public synchronized LoadResult load(HudLayout layout) {
        Objects.requireNonNull(layout, "layout");
        if (Files.notExists(hudConfigurationPath)) {
            return LoadResult.MISSING;
        }

        try (Reader reader = Files.newBufferedReader(hudConfigurationPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("HUD configuration root must be an object");
            }

            applyConfiguration(parsed.getAsJsonObject(), layout);
            return LoadResult.LOADED;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unable to load Helikon HUD configuration; using safe defaults", exception);
            layout.resetActiveModules();
            preserveMalformedConfiguration();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save(HudLayout layout) {
        Objects.requireNonNull(layout, "layout");
        JsonObject root = serializeConfiguration(layout);

        try {
            Files.createDirectories(configurationDirectory);
            Path temporaryPath = Files.createTempFile(configurationDirectory, "hud-", ".json.tmp");
            Files.writeString(temporaryPath, root.toString(), StandardCharsets.UTF_8);

            if (Files.exists(hudConfigurationPath)) {
                Files.copy(hudConfigurationPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporaryPath, hudConfigurationPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save Helikon HUD configuration", exception);
        }
    }

    private static JsonObject serializeConfiguration(HudLayout layout) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);

        JsonObject activeModules = new JsonObject();
        activeModules.addProperty("enabled", layout.activeModulesEnabled());
        activeModules.addProperty("x", layout.activeModulesX());
        activeModules.addProperty("y", layout.activeModulesY());
        root.add("activeModules", activeModules);
        return root;
    }

    private void applyConfiguration(JsonObject root, HudLayout layout) {
        int schemaVersion = getRequiredInt(root, "schemaVersion");
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported HUD configuration schema " + schemaVersion);
        }

        JsonElement activeModulesElement = root.get("activeModules");
        if (activeModulesElement == null || !activeModulesElement.isJsonObject()) {
            LOGGER.warning("Missing or invalid activeModules HUD configuration; reset to defaults");
            layout.resetActiveModules();
            return;
        }

        JsonObject activeModules = activeModulesElement.getAsJsonObject();
        boolean enabled = getOptionalBoolean(activeModules, "enabled", true);
        int x = getOptionalCoordinate(activeModules, "x", HudLayout.DEFAULT_ACTIVE_MODULES_X);
        int y = getOptionalCoordinate(activeModules, "y", HudLayout.DEFAULT_ACTIVE_MODULES_Y);

        layout.setActiveModulesEnabled(enabled);
        if (!layout.setActiveModulesPosition(x, y)) {
            LOGGER.warning("Invalid Active Modules HUD position; reset to defaults");
            layout.setActiveModulesPosition(HudLayout.DEFAULT_ACTIVE_MODULES_X, HudLayout.DEFAULT_ACTIVE_MODULES_Y);
        }
    }

    private static boolean getOptionalBoolean(JsonObject object, String property, boolean defaultValue) {
        JsonElement element = object.get(property);
        if (element == null) {
            return defaultValue;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isBoolean()) {
            return element.getAsBoolean();
        }
        LOGGER.warning(() -> "Invalid '" + property + "' HUD value; reset to default");
        return defaultValue;
    }

    private static int getOptionalCoordinate(JsonObject object, String property, int defaultValue) {
        JsonElement element = object.get(property);
        if (element == null) {
            return defaultValue;
        }
        try {
            int value = getRequiredInt(object, property);
            if (HudLayout.isValidCoordinate(value)) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            // Logged below with the same safe fallback as an out-of-range number.
        }
        LOGGER.warning(() -> "Invalid '" + property + "' HUD value; reset to default");
        return defaultValue;
    }

    private void preserveMalformedConfiguration() {
        if (Files.notExists(hudConfigurationPath)) {
            return;
        }
        Path recoveryPath = configurationDirectory.resolve("hud.corrupt-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(hudConfigurationPath, recoveryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed Helikon HUD configuration", moveException);
        }
    }

    private Path backupPath() {
        return configurationDirectory.resolve("hud.json.bak");
    }

    private static int getRequiredInt(JsonObject object, String property) {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or invalid '" + property + "'");
        }
        double parsed = element.getAsDouble();
        if (!Double.isFinite(parsed) || parsed != Math.rint(parsed) || parsed < Integer.MIN_VALUE || parsed > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Missing or invalid '" + property + "'");
        }
        return (int) parsed;
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
