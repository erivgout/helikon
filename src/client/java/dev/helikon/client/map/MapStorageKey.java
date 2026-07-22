package dev.helikon.client.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.waypoint.WaypointContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

/** Safe hashed on-disk identity for one local world/server and dimension. */
public final class MapStorageKey {
    public static final int METADATA_SCHEMA_VERSION = 1;

    private final Path root;
    private final WaypointContext context;
    private final String scopeToken;
    private final String dimensionToken;

    public MapStorageKey(Path root, WaypointContext context) {
        this.root = Objects.requireNonNull(root, "root").toAbsolutePath().normalize();
        this.context = Objects.requireNonNull(context, "context");
        scopeToken = token(context.scope());
        dimensionToken = token(context.dimension());
    }

    public WaypointContext context() {
        return context;
    }

    public String scopeToken() {
        return scopeToken;
    }

    public String dimensionToken() {
        return dimensionToken;
    }

    public Path directory() {
        Path directory = root.resolve(scopeToken).resolve(dimensionToken).normalize();
        if (!directory.startsWith(root)) {
            throw new IllegalStateException("Map context escaped its storage root");
        }
        return directory;
    }

    public Path metadataPath() {
        return directory().resolve("context.json");
    }

    public Path regionPath(int regionX, int regionZ) {
        return directory().resolve("r." + regionX + "." + regionZ + ".hmap");
    }

    /** Creates or validates self-identifying metadata without replacing mismatched data. */
    public void ensureMetadata() throws IOException {
        Path metadata = metadataPath();
        if (Files.exists(metadata)) {
            validateMetadata(Files.readString(metadata, StandardCharsets.UTF_8));
            return;
        }
        Files.createDirectories(directory());
        JsonObject rootObject = new JsonObject();
        rootObject.addProperty("schemaVersion", METADATA_SCHEMA_VERSION);
        rootObject.addProperty("scope", context.scope());
        rootObject.addProperty("dimension", context.dimension());
        Path temporary = Files.createTempFile(directory(), "context-", ".json.tmp");
        try {
            Files.writeString(temporary, rootObject.toString(), StandardCharsets.UTF_8);
            moveAtomically(temporary, metadata);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    private void validateMetadata(String content) throws IOException {
        try {
            JsonElement parsed = JsonParser.parseString(content);
            if (!parsed.isJsonObject()) {
                throw new MapMetadataException("Map context metadata root must be an object");
            }
            JsonObject object = parsed.getAsJsonObject();
            int version = requiredInt(object, "schemaVersion");
            if (version > METADATA_SCHEMA_VERSION) {
                throw new UnsupportedMetadataVersionException(version);
            }
            if (version != METADATA_SCHEMA_VERSION) {
                throw new MapMetadataException("Unsupported map context metadata schema " + version);
            }
            String scope = requiredString(object, "scope");
            String dimension = requiredString(object, "dimension");
            if (!context.scope().equals(scope) || !context.dimension().equals(dimension)
                    || !scopeToken.equals(token(scope)) || !dimensionToken.equals(token(dimension))) {
                throw new MapMetadataException("Map context metadata does not match its storage directory");
            }
        } catch (MapMetadataException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new MapMetadataException("Invalid map context metadata", exception);
        }
    }

    private static int requiredInt(JsonObject object, String property) throws MapMetadataException {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new MapMetadataException("Invalid map context metadata property '" + property + "'");
        }
        double value = element.getAsDouble();
        if (!Double.isFinite(value) || value != Math.rint(value)
                || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new MapMetadataException("Invalid map context metadata property '" + property + "'");
        }
        return (int) value;
    }

    private static String requiredString(JsonObject object, String property) throws MapMetadataException {
        JsonElement element = object.get(property);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new MapMetadataException("Invalid map context metadata property '" + property + "'");
        }
        return element.getAsString();
    }

    private static String token(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static class MapMetadataException extends IOException {
        public MapMetadataException(String message) {
            super(message);
        }

        public MapMetadataException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class UnsupportedMetadataVersionException extends MapMetadataException {
        private final int version;

        public UnsupportedMetadataVersionException(int version) {
            super("Map context metadata schema " + version + " is newer than this client supports");
            this.version = version;
        }

        public int version() {
            return version;
        }
    }
}

