package dev.helikon.client.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.module.ModuleRegistry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Local, schema-validated configuration snapshots stored below
 * {@code config/helikon/profiles}. Profile names are intentionally limited to
 * safe file-name tokens, so no user input can escape that directory.
 */
public final class ProfileManager {
    private static final Logger LOGGER = Logger.getLogger(ProfileManager.class.getName());
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    private final ConfigurationManager configuration;
    private final Path profilesDirectory;
    private final Path importsDirectory;
    private final Path exportsDirectory;
    private final Path preferencesPath;

    public ProfileManager(ConfigurationManager configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        profilesDirectory = configuration.configurationDirectory().resolve("profiles");
        importsDirectory = configuration.configurationDirectory().resolve("imports");
        exportsDirectory = configuration.configurationDirectory().resolve("exports");
        preferencesPath = configuration.configurationDirectory().resolve("profiles.json");
    }

    public Path profilesDirectory() {
        return profilesDirectory;
    }

    public Path importsDirectory() {
        return importsDirectory;
    }

    public Path exportsDirectory() {
        return exportsDirectory;
    }

    /** Returns the persisted default profile when its file still exists. */
    public synchronized Optional<String> defaultProfile() {
        return profileReference(readPreferences(), "defaultProfile");
    }

    /** Sets an existing profile as the persisted default. */
    public synchronized boolean setDefault(String name) {
        String normalizedName = normalizeName(name);
        if (!Files.isRegularFile(profilePath(normalizedName))) {
            return false;
        }
        JsonObject preferences = readPreferences();
        preferences.addProperty("defaultProfile", normalizedName);
        writePreferences(preferences);
        return true;
    }

    /** Clears the optional default-profile choice. */
    public synchronized void clearDefault() {
        JsonObject preferences = readPreferences();
        preferences.remove("defaultProfile");
        writePreferences(preferences);
    }

    /** Associates a server address with an existing local profile. */
    public synchronized boolean setServerProfile(String address, String profileName) {
        return setAssociation("serverProfiles", normalizeServerAddress(address), profileName);
    }

    public synchronized Optional<String> serverProfile(String address) {
        return associationProfile("serverProfiles", normalizeServerAddress(address));
    }

    /** Associates a singleplayer world identifier with an existing local profile. */
    public synchronized boolean setSingleplayerProfile(String worldId, String profileName) {
        return setAssociation("singleplayerProfiles", normalizeWorldId(worldId), profileName);
    }

    public synchronized Optional<String> singleplayerProfile(String worldId) {
        return associationProfile("singleplayerProfiles", normalizeWorldId(worldId));
    }

    public synchronized void clearServerProfile(String address) {
        clearAssociation("serverProfiles", normalizeServerAddress(address));
    }

    public synchronized void clearSingleplayerProfile(String worldId) {
        clearAssociation("singleplayerProfiles", normalizeWorldId(worldId));
    }

    /** Lists saved profile names in stable case-insensitive order. */
    public synchronized List<String> list() {
        if (Files.notExists(profilesDirectory)) {
            return List.of();
        }
        try (var files = Files.list(profilesDirectory)) {
            return files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.endsWith(".json") && NAME_PATTERN.matcher(name.substring(0, name.length() - 5)).matches())
                    .map(name -> name.substring(0, name.length() - 5))
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to list Helikon profiles", exception);
        }
    }

    /** Writes the current local module and ClickGUI state to a named profile. */
    public synchronized void save(String name, ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        String normalizedName = normalizeName(name);
        JsonObject snapshot = configuration.snapshot(registry, clickGuiWindow);
        snapshot.addProperty("profileName", normalizedName);

        writeSnapshot(normalizedName, snapshot, true);
    }

    /** Duplicates a validated JSON profile under a new name without activating it. */
    public synchronized boolean duplicate(String sourceName, String targetName) {
        String source = normalizeName(sourceName);
        String target = normalizeName(targetName);
        rejectSameName(source, target);
        JsonObject snapshot = readSnapshotForCopy(source);
        if (snapshot == null) {
            return false;
        }
        snapshot.addProperty("profileName", target);
        writeSnapshot(target, snapshot, false);
        return true;
    }

    /** Renames a validated JSON profile without activating it. */
    public synchronized boolean rename(String sourceName, String targetName) {
        String source = normalizeName(sourceName);
        String target = normalizeName(targetName);
        rejectSameName(source, target);
        JsonObject snapshot = readSnapshotForCopy(source);
        if (snapshot == null) {
            return false;
        }
        snapshot.addProperty("profileName", target);
        writeSnapshot(target, snapshot, false);

        try {
            updateProfileReferences(source, Optional.of(target));
            moveBackup(source, target);
            Files.delete(profilePath(source));
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to finish renaming profile '" + source + "'", exception);
        }
        return true;
    }

    /**
     * Imports a validated profile from {@code config/helikon/imports} under a
     * new name. The caller chooses only a safe token, never an arbitrary path.
     */
    public synchronized boolean importProfile(String importName, String targetName) {
        String source = normalizeName(importName);
        String target = normalizeName(targetName);
        JsonObject snapshot = readImportedSnapshot(source);
        if (snapshot == null) {
            return false;
        }
        snapshot.addProperty("profileName", target);
        writeSnapshot(target, snapshot, false);
        return true;
    }

    /** Exports a validated profile to {@code config/helikon/exports}. */
    public synchronized boolean exportProfile(String profileName, String exportName) {
        String source = normalizeName(profileName);
        String target = normalizeName(exportName);
        JsonObject snapshot = readSnapshotForCopy(source);
        if (snapshot == null) {
            return false;
        }
        writeExternalSnapshot(exportsDirectory.resolve(target + ".json"), snapshot);
        return true;
    }

    /**
     * Validates and applies a named profile. A malformed profile is preserved
     * for inspection and never partially activates module state.
     */
    public synchronized LoadResult load(String name, ModuleRegistry registry, ClickGuiWindowState clickGuiWindow) {
        String normalizedName = normalizeName(name);
        Path profilePath = profilePath(normalizedName);
        if (Files.notExists(profilePath)) {
            return LoadResult.MISSING;
        }

        try {
            // Keep filesystem failures outside Gson: its streaming parser wraps
            // a mid-read IOException in JsonIOException, which must not cause
            // a valid profile to be renamed as malformed.
            String contents = Files.readString(profilePath, StandardCharsets.UTF_8);
            JsonElement parsed = JsonParser.parseString(contents);
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Profile root must be an object");
            }
            JsonObject snapshot = parsed.getAsJsonObject();
            validateProfileName(snapshot, normalizedName);
            configuration.applySnapshot(snapshot, registry, clickGuiWindow);
            return LoadResult.LOADED;
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to read profile '" + normalizedName + "'; leaving it in place", exception);
            return LoadResult.UNAVAILABLE;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Unable to load profile '" + normalizedName + "'; preserving it without activation", exception);
            preserveMalformedProfile(normalizedName);
            return LoadResult.RECOVERED_FROM_ERROR;
        }
    }

    /** Deletes a profile and its last-known-good backup, retaining corrupt evidence files. */
    public synchronized boolean delete(String name) {
        String normalizedName = normalizeName(name);
        try {
            updateProfileReferences(normalizedName, Optional.empty());
            boolean deleted = Files.deleteIfExists(profilePath(normalizedName));
            Files.deleteIfExists(backupPath(normalizedName));
            return deleted;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to delete profile '" + normalizedName + "'", exception);
        }
    }

    public Path profilePath(String name) {
        return profilesDirectory.resolve(normalizeName(name) + ".json");
    }

    private Path backupPath(String normalizedName) {
        return profilesDirectory.resolve(normalizedName + ".json.bak");
    }

    private JsonObject readPreferences() {
        if (Files.notExists(preferencesPath)) {
            return newPreferences();
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(preferencesPath, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Profile preferences root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != 1) {
                throw new IllegalArgumentException("Unsupported profile preferences schema");
            }
            validateProfileReference(root, "defaultProfile");
            validateAssociationObject(root, "serverProfiles");
            validateAssociationObject(root, "singleplayerProfiles");
            return root;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read profile preferences", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid profile preferences; preserving them without use", exception);
            preserveMalformedPreferences();
            return newPreferences();
        }
    }

    private void writePreferences(JsonObject root) {
        root.addProperty("schemaVersion", 1);
        try {
            Files.createDirectories(configuration.configurationDirectory());
            Path temporary = Files.createTempFile(configuration.configurationDirectory(), "profiles-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(preferencesPath)) {
                Files.copy(preferencesPath, preferencesPath.resolveSibling("profiles.json.bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, preferencesPath);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save profile preferences", exception);
        }
    }

    private boolean setAssociation(String property, String key, String profileName) {
        String normalizedProfile = normalizeName(profileName);
        if (!Files.isRegularFile(profilePath(normalizedProfile))) {
            return false;
        }
        JsonObject preferences = readPreferences();
        associationObject(preferences, property).addProperty(key, normalizedProfile);
        writePreferences(preferences);
        return true;
    }

    private Optional<String> associationProfile(String property, String key) {
        JsonObject preferences = readPreferences();
        JsonElement value = associationObject(preferences, property).get(key);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            return Optional.empty();
        }
        String profileName = normalizeName(value.getAsString());
        return Files.isRegularFile(profilePath(profileName)) ? Optional.of(profileName) : Optional.empty();
    }

    private void clearAssociation(String property, String key) {
        JsonObject preferences = readPreferences();
        associationObject(preferences, property).remove(key);
        writePreferences(preferences);
    }

    private void updateProfileReferences(String source, Optional<String> target) {
        JsonObject preferences = readPreferences();
        if (storedProfileReference(preferences, "defaultProfile").filter(source::equals).isPresent()) {
            if (target.isPresent()) preferences.addProperty("defaultProfile", target.get()); else preferences.remove("defaultProfile");
        }
        for (String property : List.of("serverProfiles", "singleplayerProfiles")) {
            JsonObject associations = associationObject(preferences, property);
            for (String key : List.copyOf(associations.keySet())) {
                if (source.equals(storedProfileReference(associations, key).orElse(null))) {
                    if (target.isPresent()) associations.addProperty(key, target.get()); else associations.remove(key);
                }
            }
        }
        writePreferences(preferences);
    }

    private Optional<String> profileReference(JsonObject object, String property) {
        return storedProfileReference(object, property)
                .filter(profileName -> Files.isRegularFile(profilePath(profileName)));
    }

    private static Optional<String> storedProfileReference(JsonObject object, String property) {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) return Optional.empty();
        return Optional.of(normalizeName(element.getAsString()));
    }

    private static JsonObject newPreferences() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", 1);
        return root;
    }

    private static void validateAssociationObject(JsonObject root, String property) {
        JsonElement element = root.get(property);
        if (element != null && !element.isJsonObject()) throw new IllegalArgumentException("Invalid '" + property + "'");
        if (element != null) {
            for (var entry : element.getAsJsonObject().entrySet()) {
                if (property.equals("serverProfiles")) normalizeServerAddress(entry.getKey());
                else normalizeWorldId(entry.getKey());
                validateProfileReference(element.getAsJsonObject(), entry.getKey());
            }
        }
    }

    private static void validateProfileReference(JsonObject object, String property) {
        JsonElement element = object.get(property);
        if (element == null) return;
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid profile reference '" + property + "'");
        }
        normalizeName(element.getAsString());
    }

    private static JsonObject associationObject(JsonObject root, String property) {
        JsonElement element = root.get(property);
        if (element == null) {
            JsonObject created = new JsonObject(); root.add(property, created); return created;
        }
        return element.getAsJsonObject();
    }

    private static String normalizeServerAddress(String address) {
        return normalizeAssociation(address).toLowerCase(Locale.ROOT);
    }

    private static String normalizeWorldId(String worldId) { return normalizeAssociation(worldId); }

    private static String normalizeAssociation(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.isEmpty() || normalized.length() > 255 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Association identifiers must be 1-255 printable characters");
        }
        return normalized;
    }

    private void preserveMalformedPreferences() {
        if (Files.notExists(preferencesPath)) {
            return;
        }
        Path recoveryPath = preferencesPath.resolveSibling("profiles.corrupt-" + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(preferencesPath, recoveryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed profile preferences", exception);
        }
    }

    private static int requiredInt(JsonObject object, String property) {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Missing or invalid '" + property + "'");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Missing or invalid '" + property + "'");
        }
        return (int) value;
    }

    private JsonObject readSnapshotForCopy(String normalizedName) {
        Path source = profilePath(normalizedName);
        if (Files.notExists(source)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Profile root must be an object");
            }
            JsonObject snapshot = parsed.getAsJsonObject();
            validateProfileName(snapshot, normalizedName);
            configuration.validateSnapshot(snapshot);
            return snapshot;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read profile '" + normalizedName + "'", exception);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Profile '" + normalizedName + "' is not valid JSON", exception);
        }
    }

    private JsonObject readImportedSnapshot(String normalizedName) {
        Path source = importsDirectory.resolve(normalizedName + ".json");
        if (Files.notExists(source)) {
            return null;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Profile root must be an object");
            }
            JsonObject snapshot = parsed.getAsJsonObject();
            JsonElement profileName = snapshot.get("profileName");
            if (profileName != null && (!profileName.isJsonPrimitive()
                    || !profileName.getAsJsonPrimitive().isString())) {
                throw new IllegalArgumentException("Imported profile name is invalid");
            }
            configuration.validateSnapshot(snapshot);
            return snapshot;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read import '" + normalizedName + "'", exception);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Import '" + normalizedName + "' is not a valid profile", exception);
        }
    }

    private void writeSnapshot(String normalizedName, JsonObject snapshot, boolean replaceExisting) {
        try {
            Files.createDirectories(profilesDirectory);
            Path destination = profilePath(normalizedName);
            if (!replaceExisting && (Files.exists(destination) || Files.exists(backupPath(normalizedName)))) {
                throw new IllegalArgumentException("A profile named '" + normalizedName + "' already exists");
            }
            Path temporary = Files.createTempFile(profilesDirectory, "profile-" + normalizedName + "-", ".json.tmp");
            Files.writeString(temporary, snapshot.toString(), StandardCharsets.UTF_8);
            if (Files.exists(destination)) {
                Files.copy(destination, backupPath(normalizedName), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, destination);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save profile '" + normalizedName + "'", exception);
        }
    }

    private void writeExternalSnapshot(Path destination, JsonObject snapshot) {
        try {
            Files.createDirectories(exportsDirectory);
            Path temporary = Files.createTempFile(exportsDirectory, "profile-export-", ".json.tmp");
            Files.writeString(temporary, snapshot.toString(), StandardCharsets.UTF_8);
            if (Files.exists(destination)) {
                Files.copy(destination, destination.resolveSibling(destination.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, destination);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to export profile", exception);
        }
    }

    private static void rejectSameName(String source, String target) {
        if (source.equals(target)) {
            throw new IllegalArgumentException("Source and target profile names must differ");
        }
    }

    private void moveBackup(String source, String target) throws IOException {
        Path sourceBackup = backupPath(source);
        if (Files.exists(sourceBackup)) {
            moveAtomically(sourceBackup, backupPath(target));
        }
    }

    private void preserveMalformedProfile(String normalizedName) {
        Path profilePath = profilePath(normalizedName);
        if (Files.notExists(profilePath)) {
            return;
        }
        Path recoveryPath = profilesDirectory.resolve(normalizedName + ".corrupt-"
                + Instant.now().toEpochMilli() + ".json");
        try {
            Files.move(profilePath, recoveryPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed profile '" + normalizedName + "'", exception);
        }
    }

    private static void validateProfileName(JsonObject snapshot, String expectedName) {
        JsonElement nameElement = snapshot.get("profileName");
        if (nameElement == null) {
            return; // Accept early snapshots that did not carry a display name.
        }
        if (!nameElement.isJsonPrimitive() || !nameElement.getAsJsonPrimitive().isString()
                || !normalizeName(nameElement.getAsString()).equals(expectedName)) {
            throw new IllegalArgumentException("Profile name does not match its file name");
        }
    }

    private static String normalizeName(String name) {
        String normalized = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Profile names must use 1-32 letters, digits, '-' or '_' and start with a letter or digit");
        }
        return normalized;
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
        UNAVAILABLE,
        RECOVERED_FROM_ERROR
    }
}
