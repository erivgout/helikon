package dev.helikon.client.chat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.module.chat.ChatHistory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Bounded local per-server chat storage with atomic writes and corrupt-file recovery. */
public final class ChatHistoryManager {
    public static final int SCHEMA_VERSION = 1;
    public static final String SINGLEPLAYER_SCOPE = "singleplayer";
    private static final Logger LOGGER = Logger.getLogger(ChatHistoryManager.class.getName());
    private static final long MILLIS_PER_DAY = 86_400_000L;
    private static final int MAXIMUM_STORED_ENTRIES = 2_000;

    private final Path historiesDirectory;
    private final List<ChatHistoryEntry> entries = new ArrayList<>();
    private String scope = SINGLEPLAYER_SCOPE;
    private boolean persistenceActive;
    private boolean dirty;

    public ChatHistoryManager(Path configurationDirectory) {
        historiesDirectory = Objects.requireNonNull(configurationDirectory, "configurationDirectory").resolve("chat-history");
    }

    /** Activates a module session for the supplied local server or singleplayer scope. */
    public synchronized void activate(ChatHistory module, String requestedScope) {
        Objects.requireNonNull(module, "module");
        scope = normalizeScope(requestedScope);
        entries.clear();
        dirty = false;
        persistenceActive = module.persistentLogging();
        if (persistenceActive) {
            loadCurrent(module, System.currentTimeMillis());
        }
    }

    /** Saves an active persisted session before the module stops retaining new history. */
    public synchronized void deactivate() {
        saveIfNeeded();
        persistenceActive = false;
        dirty = false;
    }

    /** Applies setting changes without writing once per rendered chat line. */
    public synchronized void updateSettings(ChatHistory module) {
        Objects.requireNonNull(module, "module");
        if (!module.isEnabled()) {
            return;
        }
        if (persistenceActive && !module.persistentLogging()) {
            saveIfNeeded();
            persistenceActive = false;
            dirty = false;
        } else if (!persistenceActive && module.persistentLogging()) {
            entries.clear();
            dirty = false;
            persistenceActive = true;
            loadCurrent(module, System.currentTimeMillis());
        }
        prune(module, System.currentTimeMillis());
    }

    /** Changes local scope on a normal connection change, saving only at that lifecycle boundary. */
    public synchronized void switchScope(ChatHistory module, String requestedScope) {
        Objects.requireNonNull(module, "module");
        String normalized = normalizeScope(requestedScope);
        if (scope.equals(normalized)) {
            return;
        }
        saveIfNeeded();
        scope = normalized;
        entries.clear();
        dirty = false;
        persistenceActive = module.isEnabled() && module.persistentLogging();
        if (persistenceActive) {
            loadCurrent(module, System.currentTimeMillis());
        }
    }

    /** Records an already allowed incoming line in memory and, only if opted in, for a later local save. */
    public synchronized void recordIncoming(ChatHistory module, IncomingChatMessage message) {
        Objects.requireNonNull(message, "message");
        String text = sanitizeText(message.text());
        if (text != null) {
            record(module, new ChatHistoryEntry(message.receivedAtMillis(), ChatHistoryEntry.Direction.INCOMING,
                    message.sender(), text), System.currentTimeMillis());
        }
    }

    /** Records one already accepted ordinary outgoing chat line; local commands never reach this path. */
    public synchronized void recordOutgoing(ChatHistory module, String text, long sentAtMillis) {
        String safeText = sanitizeText(text);
        if (safeText != null) {
            record(module, new ChatHistoryEntry(sentAtMillis, ChatHistoryEntry.Direction.OUTGOING, "", safeText),
                    System.currentTimeMillis());
        }
    }

    /** Testable local recording boundary; no file operation occurs here. */
    public synchronized void record(ChatHistory module, ChatHistoryEntry entry, long nowMillis) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(entry, "entry");
        if (!module.isEnabled()) {
            return;
        }
        entries.addFirst(entry);
        prune(module, nowMillis);
        if (persistenceActive) {
            dirty = true;
        }
    }

    public synchronized List<ChatHistoryEntry> entries() {
        return List.copyOf(entries);
    }

    public synchronized String scope() {
        return scope;
    }

    /** Writes only a dirty opted-in history, for client stop or normal connection changes. */
    public synchronized void saveIfNeeded() {
        if (!persistenceActive || !dirty) {
            return;
        }
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", SCHEMA_VERSION);
        root.addProperty("scope", scope);
        JsonArray savedEntries = new JsonArray();
        for (ChatHistoryEntry entry : entries) {
            JsonObject object = new JsonObject();
            object.addProperty("timestampMillis", entry.timestampMillis());
            object.addProperty("direction", entry.direction().name().toLowerCase(Locale.ROOT));
            object.addProperty("sender", entry.sender());
            object.addProperty("text", entry.text());
            savedEntries.add(object);
        }
        root.add("entries", savedEntries);
        Path destination = currentPath();
        try {
            Files.createDirectories(historiesDirectory);
            Path temporary = Files.createTempFile(historiesDirectory, "chat-history-", ".json.tmp");
            Files.writeString(temporary, root.toString(), StandardCharsets.UTF_8);
            if (Files.exists(destination)) {
                Files.copy(destination, backupPath(destination), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, destination);
            dirty = false;
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to save local chat history", exception);
        }
    }

    public Path pathForScope(String requestedScope) {
        return historiesDirectory.resolve(scopeToken(normalizeScope(requestedScope)) + ".json");
    }

    private void loadCurrent(ChatHistory module, long nowMillis) {
        Path source = currentPath();
        if (Files.notExists(source)) {
            return;
        }
        try {
            JsonElement parsed = JsonParser.parseString(Files.readString(source, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
                throw new IllegalArgumentException("Chat history root must be an object");
            }
            JsonObject root = parsed.getAsJsonObject();
            if (requiredInt(root, "schemaVersion") != SCHEMA_VERSION) {
                throw new IllegalArgumentException("Unsupported chat history schema");
            }
            if (!scope.equals(normalizeScope(requiredString(root, "scope")))) {
                throw new IllegalArgumentException("Chat history scope does not match its file");
            }
            JsonElement savedEntries = root.get("entries");
            if (savedEntries == null || !savedEntries.isJsonArray()) {
                throw new IllegalArgumentException("Missing chat history entries");
            }
            entries.clear();
            for (JsonElement savedEntry : savedEntries.getAsJsonArray()) {
                if (entries.size() >= MAXIMUM_STORED_ENTRIES) {
                    throw new IllegalArgumentException("Chat history entry limit exceeded");
                }
                entries.add(parseEntry(savedEntry));
            }
            prune(module, nowMillis);
        } catch (IOException exception) {
            throw new ConfigurationException("Unable to read local chat history", exception);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Invalid chat history file; preserving it without use", exception);
            preserveMalformed(source);
            entries.clear();
            dirty = false;
        }
    }

    private void prune(ChatHistory module, long nowMillis) {
        long retentionMillis = Math.multiplyExact((long) module.retentionDays(), MILLIS_PER_DAY);
        long cutoff = Math.max(0L, nowMillis - retentionMillis);
        int originalSize = entries.size();
        entries.removeIf(entry -> entry.timestampMillis() < cutoff);
        if (entries.size() > module.historyLimit()) {
            entries.subList(module.historyLimit(), entries.size()).clear();
        }
        if (persistenceActive && entries.size() != originalSize) {
            dirty = true;
        }
    }

    private ChatHistoryEntry parseEntry(JsonElement element) {
        if (!element.isJsonObject()) {
            throw new IllegalArgumentException("Invalid chat history entry");
        }
        JsonObject object = element.getAsJsonObject();
        long timestamp = requiredLong(object, "timestampMillis");
        String direction = requiredString(object, "direction");
        ChatHistoryEntry.Direction parsedDirection = switch (direction.toLowerCase(Locale.ROOT)) {
            case "incoming" -> ChatHistoryEntry.Direction.INCOMING;
            case "outgoing" -> ChatHistoryEntry.Direction.OUTGOING;
            default -> throw new IllegalArgumentException("Invalid chat history direction");
        };
        return new ChatHistoryEntry(timestamp, parsedDirection, requiredString(object, "sender"),
                requiredString(object, "text"));
    }

    private Path currentPath() {
        return pathForScope(scope);
    }

    private static Path backupPath(Path path) {
        return path.resolveSibling(path.getFileName() + ".bak");
    }

    private void preserveMalformed(Path path) {
        if (Files.notExists(path)) {
            return;
        }
        try {
            Files.move(path, path.resolveSibling(path.getFileName().toString().replace(".json", "")
                    + ".corrupt-" + Instant.now().toEpochMilli() + ".json"), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            LOGGER.log(Level.WARNING, "Unable to preserve malformed chat history", exception);
        }
    }

    private static String sanitizeText(String text) {
        if (text == null) {
            return null;
        }
        String normalized = text.replace('\r', ' ').replace('\n', ' ').trim();
        if (normalized.isEmpty() || normalized.length() > ChatHistoryEntry.MAXIMUM_TEXT_LENGTH) {
            return null;
        }
        return normalized;
    }

    private static String normalizeScope(String value) {
        String scope = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        if (scope.isEmpty()) {
            return SINGLEPLAYER_SCOPE;
        }
        if (scope.length() > 255 || !scope.matches("[a-z0-9.:_-]+")) {
            throw new IllegalArgumentException("Chat history scope must be a safe local server token");
        }
        return scope;
    }

    private static String scopeToken(String scope) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(scope.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String requiredString(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return value.getAsString();
    }

    private static int requiredInt(JsonObject object, String property) {
        long value = requiredLong(object, property);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return (int) value;
    }

    private static long requiredLong(JsonObject object, String property) {
        JsonElement value = object.get(property);
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number != Math.rint(number) || number < 0L || number > Long.MAX_VALUE) {
            throw new IllegalArgumentException("Invalid '" + property + "'");
        }
        return (long) number;
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
