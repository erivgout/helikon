package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.input.Keybind;
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
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists the initial global module state in human-readable JSON under
 * {@code config/helikon/global.json}. More local data stores will be added in
 * later milestones.
 */
public final class ConfigurationManager {
    public static final int SCHEMA_VERSION = 1;

    private static final Logger LOGGER = Logger.getLogger(ConfigurationManager.class.getName());

    private final Path configurationDirectory;
    private final Path globalConfigurationPath;

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

    public synchronized LoadResult load(ModuleRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        if (Files.notExists(globalConfigurationPath)) {
            return LoadResult.MISSING;
        }

        try (Reader reader = Files.newBufferedReader(globalConfigurationPath, StandardCharsets.UTF_8)) {
            JsonElement parsed = JsonParser.parseReader(reader);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Global configuration root must be an object");
            }

            applyConfiguration(parsed.getAsJsonObject(), registry);
            return LoadResult.LOADED;
        } catch (Exception exception) {
            LOGGER.log(Level.WARNING, "Unable to load Helikon configuration; using safe defaults", exception);
            preserveMalformedConfiguration();
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    public synchronized void save(ModuleRegistry registry) {
        Objects.requireNonNull(registry, "registry");
        JsonObject root = serializeConfiguration(registry);

        try {
            Files.createDirectories(configurationDirectory);
            Path temporaryPath = Files.createTempFile(configurationDirectory, "global-", ".json.tmp");
            Files.writeString(temporaryPath, root.toString(), StandardCharsets.UTF_8);

            if (Files.exists(globalConfigurationPath)) {
                Files.copy(globalConfigurationPath, backupPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporaryPath, globalConfigurationPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save Helikon configuration", exception);
        }
    }

    private JsonObject serializeConfiguration(ModuleRegistry registry) {
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
        return root;
    }

    private void applyConfiguration(JsonObject root, ModuleRegistry registry) {
        int schemaVersion = getRequiredInt(root, "schemaVersion");
        if (schemaVersion < 1 || schemaVersion > SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported configuration schema " + schemaVersion);
        }

        JsonObject modules = getRequiredObject(root, "modules");
        for (Module module : registry.all()) {
            JsonElement moduleElement = modules.get(module.id());
            if (moduleElement == null || !moduleElement.isJsonObject()) {
                registry.setEnabled(module, module.defaultEnabled());
                continue;
            }

            JsonObject moduleObject = moduleElement.getAsJsonObject();
            JsonElement settingsElement = moduleObject.get("settings");
            if (settingsElement != null && settingsElement.isJsonObject()) {
                applySettings(module, settingsElement.getAsJsonObject());
            }

            applyKeybind(module, moduleObject.get("keybind"));

            JsonElement enabledElement = moduleObject.get("enabled");
            boolean enabled = enabledElement != null && enabledElement.isJsonPrimitive()
                    && enabledElement.getAsJsonPrimitive().isBoolean()
                    ? enabledElement.getAsBoolean()
                    : module.defaultEnabled();
            registry.setEnabled(module, enabled);
        }
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

    private static JsonObject serializeKeybind(Keybind keybind) {
        JsonObject keybindObject = new JsonObject();
        keybindObject.addProperty("key", keybind.keyCode());
        keybindObject.addProperty("activation", keybind.activation().name().toLowerCase(Locale.ROOT));
        return keybindObject;
    }

    /** Applies a stored keybind; missing or invalid data leaves the module unbound-safe default. */
    private static void applyKeybind(Module module, JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            return;
        }

        try {
            JsonObject keybindObject = element.getAsJsonObject();
            int keyCode = getRequiredInt(keybindObject, "key");
            JsonElement activationElement = keybindObject.get("activation");
            if (activationElement == null || !activationElement.isJsonPrimitive()
                    || !activationElement.getAsJsonPrimitive().isString()) {
                throw new IllegalArgumentException("Missing or invalid 'activation'");
            }
            Keybind.Activation activation = Keybind.Activation.valueOf(
                    activationElement.getAsString().toUpperCase(Locale.ROOT));
            // The Keybind constructor rejects key codes outside the supported
            // range, sending manually edited garbage to the catch below.
            module.setKeybind(new Keybind(keyCode, activation));
        } catch (RuntimeException exception) {
            LOGGER.warning(() -> "Invalid keybind for module '" + module.id() + "'; keeping it unbound");
            module.setKeybind(Keybind.unbound());
        }
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
