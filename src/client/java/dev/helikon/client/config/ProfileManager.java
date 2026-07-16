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

    public ProfileManager(ConfigurationManager configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        profilesDirectory = configuration.configurationDirectory().resolve("profiles");
    }

    public Path profilesDirectory() {
        return profilesDirectory;
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
            moveBackup(source, target);
            Files.delete(profilePath(source));
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to finish renaming profile '" + source + "'", exception);
        }
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
