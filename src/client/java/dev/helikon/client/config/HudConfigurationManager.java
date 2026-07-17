package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.hud.ActiveModulesLayout;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudElementPlacement;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the small, schema-versioned HUD layout in {@code hud.json}. It uses
 * the same local-only atomic-write and corrupt-file recovery guarantees as the
 * global module configuration.
 */
public final class HudConfigurationManager {
    public static final int SCHEMA_VERSION = 4;

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
            layout.resetElements();
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

        ActiveModulesLayout state = layout.activeModules();
        JsonObject activeModules = new JsonObject();
        activeModules.addProperty("enabled", state.enabled());
        activeModules.addProperty("x", state.x());
        activeModules.addProperty("y", state.y());
        activeModules.addProperty("scale", state.scale());
        activeModules.addProperty("padding", state.padding());
        activeModules.addProperty("background", state.background());
        activeModules.addProperty("textShadow", state.textShadow());
        activeModules.addProperty("sort", state.sort().name().toLowerCase(Locale.ROOT));
        activeModules.addProperty("alignment", state.alignment().name().toLowerCase(Locale.ROOT));
        activeModules.addProperty("colorMode", state.colorMode().name().toLowerCase(Locale.ROOT));
        activeModules.addProperty("animations", state.animations());
        root.add("activeModules", activeModules);

        JsonObject elements = new JsonObject();
        for (HudElementId element : HudElementId.values()) {
            HudElementPlacement placement = layout.element(element);
            JsonObject value = new JsonObject();
            value.addProperty("enabled", placement.enabled());
            value.addProperty("anchor", placement.anchor().name().toLowerCase(Locale.ROOT));
            value.addProperty("x", placement.offsetX());
            value.addProperty("y", placement.offsetY());
            value.addProperty("scale", placement.scale());
            value.addProperty("alignment", placement.alignment().name().toLowerCase(Locale.ROOT));
            value.addProperty("background", placement.background());
            value.addProperty("padding", placement.padding());
            value.addProperty("textShadow", placement.textShadow());
            value.addProperty("color", placement.color());
            value.addProperty("rainbow", placement.rainbow());
            elements.add(element.name().toLowerCase(Locale.ROOT), value);
        }
        root.add("elements", elements);
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
            layout.resetElements();
            return;
        }

        JsonObject activeModules = activeModulesElement.getAsJsonObject();
        ActiveModulesLayout state = layout.activeModules();
        state.reset();
        state.setEnabled(getOptionalBoolean(activeModules, "enabled", true));
        int x = getOptionalCoordinate(activeModules, "x", HudLayout.DEFAULT_ACTIVE_MODULES_X);
        int y = getOptionalCoordinate(activeModules, "y", HudLayout.DEFAULT_ACTIVE_MODULES_Y);
        if (!state.setPosition(x, y)) {
            LOGGER.warning("Invalid Active Modules HUD position; reset to defaults");
            state.setPosition(HudLayout.DEFAULT_ACTIVE_MODULES_X, HudLayout.DEFAULT_ACTIVE_MODULES_Y);
        }
        state.setScale(getOptionalScale(activeModules));
        state.setPadding(getOptionalPadding(activeModules));
        state.setBackground(getOptionalBoolean(activeModules, "background", true));
        state.setTextShadow(getOptionalBoolean(activeModules, "textShadow", true));
        state.setSort(getOptionalEnum(activeModules, "sort", ActiveModulesLayout.Sort.class,
                ActiveModulesLayout.Sort.REGISTRY));
        state.setAlignment(getOptionalEnum(activeModules, "alignment", ActiveModulesLayout.Alignment.class,
                ActiveModulesLayout.Alignment.LEFT));
        state.setColorMode(getOptionalEnum(activeModules, "colorMode", ActiveModulesLayout.ColorMode.class,
                ActiveModulesLayout.ColorMode.ACCENT));
        state.setAnimations(getOptionalBoolean(activeModules, "animations", true));
        applyElementPlacements(root.get("elements"), layout);
    }

    private static void applyElementPlacements(JsonElement element, HudLayout layout) {
        layout.resetElements();
        if (element == null) {
            return;
        }
        if (!element.isJsonObject()) {
            LOGGER.warning("Invalid HUD element placement block; using defaults");
            return;
        }
        JsonObject values = element.getAsJsonObject();
        for (HudElementId id : HudElementId.values()) {
            JsonElement value = values.get(id.name().toLowerCase(Locale.ROOT));
            if (value == null) {
                continue;
            }
            if (!value.isJsonObject()) {
                LOGGER.warning(() -> "Invalid HUD placement for '" + id.name().toLowerCase(Locale.ROOT) + "'; reset");
                continue;
            }
            JsonObject object = value.getAsJsonObject();
            HudElementPlacement placement = layout.element(id);
            placement.setEnabled(getOptionalBoolean(object, "enabled", true));
            HudElementId.Anchor anchor = getOptionalEnum(object, "anchor", HudElementId.Anchor.class, id.defaultAnchor());
            int x = getOptionalCoordinate(object, "x", id.defaultOffsetX());
            int y = getOptionalCoordinate(object, "y", id.defaultOffsetY());
            if (!placement.set(anchor, x, y)) {
                LOGGER.warning(() -> "Invalid HUD placement for '" + id.name().toLowerCase(Locale.ROOT) + "'; reset");
                placement.reset(id);
                continue;
            }
            placement.setScale(getOptionalElementScale(object));
            placement.setAlignment(getOptionalEnum(object, "alignment", HudElementPlacement.Alignment.class,
                    HudElementPlacement.Alignment.LEFT));
            placement.setBackground(getOptionalBoolean(object, "background", true));
            placement.setPadding(getOptionalElementPadding(object));
            placement.setTextShadow(getOptionalBoolean(object, "textShadow", true));
            placement.setColor(getOptionalColor(object, "color", HudElementPlacement.DEFAULT_COLOR));
            placement.setRainbow(getOptionalBoolean(object, "rainbow", false));
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

    private static float getOptionalScale(JsonObject object) {
        JsonElement element = object.get("scale");
        if (element == null) {
            return 1.0F;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            float value = element.getAsFloat();
            if (Float.isFinite(value) && value >= ActiveModulesLayout.MIN_SCALE && value <= ActiveModulesLayout.MAX_SCALE) {
                return value;
            }
        }
        LOGGER.warning("Invalid 'scale' HUD value; reset to default");
        return 1.0F;
    }

    private static int getOptionalPadding(JsonObject object) {
        JsonElement element = object.get("padding");
        if (element == null) {
            return ActiveModulesHud.PADDING;
        }
        try {
            int value = getRequiredInt(object, "padding");
            if (value >= ActiveModulesLayout.MIN_PADDING && value <= ActiveModulesLayout.MAX_PADDING) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            // The shared safe fallback below also covers fractional values.
        }
        LOGGER.warning("Invalid 'padding' HUD value; reset to default");
        return ActiveModulesHud.PADDING;
    }

    private static float getOptionalElementScale(JsonObject object) {
        JsonElement element = object.get("scale");
        if (element == null) {
            return 1.0F;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isNumber()) {
            float value = element.getAsFloat();
            if (Float.isFinite(value) && value >= HudElementPlacement.MIN_SCALE
                    && value <= HudElementPlacement.MAX_SCALE) {
                return value;
            }
        }
        LOGGER.warning("Invalid HUD element scale; reset to default");
        return 1.0F;
    }

    private static int getOptionalElementPadding(JsonObject object) {
        JsonElement element = object.get("padding");
        if (element == null) {
            return 3;
        }
        try {
            int value = getRequiredInt(object, "padding");
            if (value >= HudElementPlacement.MIN_PADDING && value <= HudElementPlacement.MAX_PADDING) {
                return value;
            }
        } catch (IllegalArgumentException ignored) {
            // Logged below.
        }
        LOGGER.warning("Invalid HUD element padding; reset to default");
        return 3;
    }

    private static int getOptionalColor(JsonObject object, String property, int defaultValue) {
        JsonElement element = object.get(property);
        if (element == null) {
            return defaultValue;
        }
        try {
            return getRequiredInt(object, property);
        } catch (IllegalArgumentException ignored) {
            LOGGER.warning(() -> "Invalid '" + property + "' HUD value; reset to default");
            return defaultValue;
        }
    }

    private static <E extends Enum<E>> E getOptionalEnum(JsonObject object, String property, Class<E> type, E defaultValue) {
        JsonElement element = object.get(property);
        if (element == null) {
            return defaultValue;
        }
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            try {
                return Enum.valueOf(type, element.getAsString().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                // Logged below.
            }
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
