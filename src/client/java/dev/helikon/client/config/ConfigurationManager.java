package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.gui.ClickGuiTheme;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.Setting;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the initial global module state in human-readable JSON under
 * {@code config/helikon/global.json}. More local data stores will be added in
 * later milestones.
 */
public final class ConfigurationManager {
    public static final int SCHEMA_VERSION = 1;

    /** Legacy bootstrap IDs that retain their saved state after graduating to a real module. */
    private static final Map<String, String> LEGACY_MODULE_IDS = Map.of("fullbright", "fullbright_stub");

    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());

    private final Path configurationDirectory;
    private final Path globalConfigurationPath;
    private volatile SaveStatus saveStatus = SaveStatus.NOT_SAVED;

    public ConfigurationManager(Path configurationDirectory) {
        this.configurationDirectory = Objects.requireNonNull(configurationDirectory, "configurationDirectory").normalize();
        this.globalConfigurationPath = this.configurationDirectory.resolve("global.json");
    }

    public Path configurationDirectory() {
        return configurationDirectory;
    }

    public Path globalConfigurationPath() {
        return globalConfigurationPath;
    }

    /** Current local global-configuration write state for the opt-in diagnostics HUD. */
    public SaveStatus saveStatus() {
        return saveStatus;
    }

    public synchronized LoadResult load(ModuleRegistry registry) {
        return load(registry, null);
    }

    /** Loads module state plus an optional persisted ClickGUI window position. */
    public synchronized LoadResult load(ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        Objects.requireNonNull(registry, "registry");
        if (Files.notExists(globalConfigurationPath)) {
            if (clickGuiWindow != null) {
                clickGuiWindow.reset();
            }
            return LoadResult.MISSING;
        }

        try (Reader reader = Files.newBufferedReader(globalConfigurationPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Global configuration root must be an object");
            }

            applyConfiguration(parsed.getAsJsonObject(), registry, clickGuiWindow);
            return LoadResult.LOADED;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unable to load Helikon configuration; using safe defaults", exception);
            if (clickGuiWindow != null) {
                clickGuiWindow.reset();
            }
            preserveMalformedConfiguration();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save(ModuleRegistry registry) {
        save(registry, null);
    }

    /** Saves module state plus an optional ClickGUI window position. */
    public synchronized void save(ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        Objects.requireNonNull(registry, "registry");
        saveStatus = SaveStatus.SAVING;
        try {
            JsonObject root = serializeConfiguration(registry, clickGuiWindow);
            Files.createDirectories(configurationDirectory);
            Path temporaryPath = Files.createTempFile(configurationDirectory, "global-", ".json.tmp");
            Files.writeString(temporaryPath, root.toString(), StandardCharsets.UTF_8);

            if (Files.exists(globalConfigurationPath)) {
                Files.copy(globalConfigurationPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporaryPath, globalConfigurationPath);
            saveStatus = SaveStatus.SAVED;
        } catch (IOException exception) {
            saveStatus = SaveStatus.FAILED;
            throw new ConfigurationException("Unable to save Helikon configuration", exception);
        } catch (RuntimeException exception) {
            saveStatus = SaveStatus.FAILED;
            throw exception;
        }
    }

    /** Local global-configuration save state; this is never persisted or transmitted. */
    public enum SaveStatus {
        NOT_SAVED("not saved"),
        SAVING("saving"),
        SAVED("saved"),
        FAILED("failed");

        private final String displayName;

        SaveStatus(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }
    }

    /** Creates a validated-schema configuration snapshot for a local profile. */
    public synchronized JsonObject snapshot(ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        Objects.requireNonNull(registry, "registry");
        return serializeConfiguration(registry, clickGuiWindow);
    }

    /**
     * Applies a previously parsed local profile snapshot. Structural validation
     * happens before module state changes; individual invalid settings retain
     * the existing safe-default recovery behavior.
     */
    public synchronized void applySnapshot(
            JsonObject snapshot,
            ModuleRegistry registry,
            ClickGuiWindowState clickGuiWindow
    ) {
        applyConfiguration(Objects.requireNonNull(snapshot, "snapshot"),
                Objects.requireNonNull(registry, "registry"), clickGuiWindow);
    }

    /** Validates profile-level schema without changing module or GUI state. */
    public synchronized void validateSnapshot(JsonObject snapshot) {
        validateSnapshotStructure(Objects.requireNonNull(snapshot, "snapshot"));
    }

    private JsonObject serializeConfiguration(ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);

        JsonObject modules = new JsonObject();
        for (Module module : registry.all()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("enabled", module.isEnabled());
            moduleObject.add("keybind", serializeKeybind(module.keybind()));

            JsonObject settings = new JsonObject();
            for (Setting<?> setting : module.settings()) {
                settings.add(setting.id(), setting.toJson());
            }
            moduleObject.add("settings", settings);
            modules.add(module.id(), moduleObject);
        }
        root.add("modules", modules);

        if (clickGuiWindow != null) {
            root.add("clickGui", serializeClickGuiWindow(clickGuiWindow));
        }
        return root;
    }

    private void applyConfiguration(JsonObject root, ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        validateSnapshotStructure(root);

        JsonObject modules = root.getAsJsonObject("modules");
        for (Module module : registry.all()) {
            JsonElement moduleElement = modules.get(module.id());
            if (moduleElement == null) {
                String legacyId = LEGACY_MODULE_IDS.get(module.id());
                if (legacyId != null) {
                    moduleElement = modules.get(legacyId);
                    if (moduleElement != null) {
                        LOGGER.info(() -> "Migrating saved module state from '" + legacyId + "' to '" + module.id() + "'");
                    }
                }
            }
            if (moduleElement == null || !moduleElement.isJsonObject()) {
                restoreModuleDefaults(module);
                registry.setEnabled(module, module.defaultEnabled());
                continue;
            }

            JsonObject moduleObject = moduleElement.getAsJsonObject();
            JsonElement settingsElement = moduleObject.get("settings");
            if (settingsElement != null && settingsElement.isJsonObject()) {
                applySettings(module, settingsElement.getAsJsonObject());
            } else {
                if (settingsElement != null) {
                    LOGGER.warning(() -> "Invalid settings block for module '" + module.id() + "'; reset to defaults");
                }
                module.resetSettings();
            }

            applyKeybind(module, moduleObject.get("keybind"));

            JsonElement enabledElement = moduleObject.get("enabled");
            boolean enabled = enabledElement != null && enabledElement.isJsonPrimitive()
                    && enabledElement.getAsJsonPrimitive().isBoolean()
                    ? enabledElement.getAsBoolean()
                    : module.defaultEnabled();
            registry.setEnabled(module, enabled);
        }

        if (clickGuiWindow != null) {
            applyClickGuiWindow(root.get("clickGui"), clickGuiWindow);
        }
    }

    private static void validateSnapshotStructure(JsonObject root) {
        int schemaVersion = getRequiredInt(root, "schemaVersion");
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported configuration schema " + schemaVersion);
        }
        getRequiredObject(root, "modules");
    }

    private void applySettings(Module module, JsonObject serializedSettings) {
        for (Setting<?> setting : module.settings()) {
            JsonElement settingElement = serializedSettings.get(setting.id());
            if (settingElement == null) {
                setting.reset();
                continue;
            }
            if (!setting.applyJson(settingElement)) {
                LOGGER.warning(() -> "Invalid value for setting '" + module.id() + "." + setting.id() + "'; reset to default");
            }
        }
    }

    private static void restoreModuleDefaults(Module module) {
        module.resetSettings();
        module.setKeybind(module.defaultKeybind());
    }

    private static JsonObject serializeClickGuiWindow(ClickGuiWindowState clickGuiWindow) {
        JsonObject window = new JsonObject();
        window.addProperty("positioned", clickGuiWindow.isPositioned());
        if (clickGuiWindow.isPositioned()) {
            window.addProperty("x", clickGuiWindow.x());
            window.addProperty("y", clickGuiWindow.y());
        }
        window.addProperty("sized", clickGuiWindow.isSized());
        if (clickGuiWindow.isSized()) {
            window.addProperty("width", clickGuiWindow.width());
            window.addProperty("height", clickGuiWindow.height());
        }
        window.addProperty("theme", clickGuiWindow.theme().id());
        window.addProperty("interfaceScale", clickGuiWindow.interfaceScale());
        window.addProperty("reducedAnimations", clickGuiWindow.reducedAnimations());
        return window;
    }

    /** Applies an optional layout block; malformed position and size values use independent safe defaults. */
    private void applyClickGuiWindow(JsonElement element, ClickGuiWindowState clickGuiWindow) {
        if (element == null) {
            clickGuiWindow.reset();
            return;
        }
        if (!element.isJsonObject()) {
            LOGGER.warning("Invalid ClickGUI layout; reset to centered placement");
            clickGuiWindow.reset();
            return;
        }

        JsonObject window = element.getAsJsonObject();
        clickGuiWindow.reset();
        JsonElement positionedElement = window.get("positioned");
        if (positionedElement == null || !positionedElement.isJsonPrimitive()
                || !positionedElement.getAsJsonPrimitive().isBoolean()) {
            LOGGER.warning("Invalid ClickGUI positioned value; reset to centered placement");
            clickGuiWindow.reset();
            return;
        }
        if (!positionedElement.getAsBoolean()) {
            clickGuiWindow.resetPosition();
        } else {
            try {
                int x = getRequiredInt(window, "x");
                int y = getRequiredInt(window, "y");
                if (!clickGuiWindow.setPosition(x, y)) {
                    throw new IllegalArgumentException("ClickGUI coordinates are outside the supported range");
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warning("Invalid ClickGUI position; reset to centered placement");
                clickGuiWindow.resetPosition();
            }
        }

        JsonElement sizedElement = window.get("sized");
        if (sizedElement != null && (!sizedElement.isJsonPrimitive() || !sizedElement.getAsJsonPrimitive().isBoolean())) {
            LOGGER.warning("Invalid ClickGUI sized value; reset to default dimensions");
            clickGuiWindow.resetSize();
        } else if (sizedElement != null && sizedElement.getAsBoolean()) {
            try {
                int width = getRequiredInt(window, "width");
                int height = getRequiredInt(window, "height");
                if (!clickGuiWindow.setSize(width, height)) {
                    throw new IllegalArgumentException("ClickGUI dimensions are outside the supported range");
                }
            } catch (IllegalArgumentException exception) {
                LOGGER.warning("Invalid ClickGUI dimensions; reset to defaults");
                clickGuiWindow.resetSize();
            }
        }

        JsonElement themeElement = window.get("theme");
        if (themeElement == null) {
            return;
        }
        if (!themeElement.isJsonPrimitive() || !themeElement.getAsJsonPrimitive().isString()) {
            LOGGER.warning("Invalid ClickGUI theme; reset to Midnight");
            return;
        }
        ClickGuiTheme.find(themeElement.getAsString()).ifPresentOrElse(clickGuiWindow::setTheme,
                () -> LOGGER.warning("Unknown ClickGUI theme; reset to Midnight"));

        JsonElement scaleElement = window.get("interfaceScale");
        if (scaleElement != null) {
            if (!scaleElement.isJsonPrimitive() || !scaleElement.getAsJsonPrimitive().isNumber()
                    || !clickGuiWindow.setInterfaceScale(scaleElement.getAsFloat())) {
                LOGGER.warning("Invalid ClickGUI interface scale; reset to 1.0");
            }
        }
        JsonElement reducedAnimationsElement = window.get("reducedAnimations");
        if (reducedAnimationsElement != null) {
            if (!reducedAnimationsElement.isJsonPrimitive()
                    || !reducedAnimationsElement.getAsJsonPrimitive().isBoolean()) {
                LOGGER.warning("Invalid ClickGUI reduced animations value; reset to off");
            } else {
                clickGuiWindow.setReducedAnimations(reducedAnimationsElement.getAsBoolean());
            }
        }
    }

    private static JsonObject serializeKeybind(Keybind keybind) {
        JsonObject keybindObject = new JsonObject();
        keybindObject.addProperty("inputType", keybind.inputType().name().toLowerCase(Locale.ROOT));
        keybindObject.addProperty("key", keybind.keyCode());
        keybindObject.addProperty("activation", keybind.activation().name().toLowerCase(Locale.ROOT));
        JsonArray modifiers = new JsonArray();
        keybind.modifiers().stream().sorted().forEach(modifier ->
                modifiers.add(modifier.name().toLowerCase(Locale.ROOT)));
        keybindObject.add("modifiers", modifiers);
        return keybindObject;
    }

    /** Applies a stored keybind; missing or invalid data leaves the module unbound-safe default. */
    private static void applyKeybind(Module module, JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            module.setKeybind(module.defaultKeybind());
            return;
        }

        try {
            JsonObject keybindObject = element.getAsJsonObject();
            int keyCode = getRequiredInt(keybindObject, "key");
            Keybind.InputType inputType = keybindObject.has("inputType")
                    ? Keybind.InputType.valueOf(getRequiredString(keybindObject, "inputType").toUpperCase(Locale.ROOT))
                    : Keybind.InputType.KEYBOARD;
            JsonElement activationElement = keybindObject.get("activation");
            if (activationElement == null || !activationElement.isJsonPrimitive()
                    || !activationElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Missing or invalid 'activation'");
            }
            Keybind.Activation activation = Keybind.Activation.valueOf(
                    activationElement.getAsString().toUpperCase(Locale.ROOT));
            Set<Keybind.Modifier> modifiers = parseKeybindModifiers(keybindObject.get("modifiers"));
            // The Keybind constructor rejects key codes outside the supported
            // range, sending manually edited garbage to the catch below.
            module.setKeybind(new Keybind(inputType, keyCode, modifiers, activation));
        } catch (RuntimeException exception) {
            LOGGER.warning(() -> "Invalid keybind for module '" + module.id() + "'; keeping it unbound");
            module.setKeybind(Keybind.unbound());
        }
    }

    private static Set<Keybind.Modifier> parseKeybindModifiers(JsonElement element) {
        if (element == null) {
            return Set.of();
        }
        if (!element.isJsonArray()) {
            throw new IllegalArgumentException("Invalid keybind modifiers");
        }
        EnumSet<Keybind.Modifier> modifiers = EnumSet.noneOf(Keybind.Modifier.class);
        for (JsonElement modifier : element.getAsJsonArray()) {
            if (!modifier.isJsonPrimitive() || !modifier.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Invalid keybind modifier");
            }
            modifiers.add(Keybind.Modifier.valueOf(modifier.getAsString().toUpperCase(Locale.ROOT)));
        }
        return Set.copyOf(modifiers);
    }

    private void preserveMalformedConfiguration() {
        if (Files.notExists(globalConfigurationPath)) {
            return;
        }
        Path recoveryPath = configurationDirectory.resolve("global.corrupt-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(globalConfigurationPath, recoveryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException moveException) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed Helikon configuration", moveException);
        }
    }

    private Path backupPath() {
        return configurationDirectory.resolve("global.json.bak");
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

    private static String getRequiredString(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return value.getAsString();
    }

    private static JsonObject getRequiredObject(JsonObject object, String property) {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Missing or invalid '" + property + "'");
        }
        return element.getAsJsonObject();
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
