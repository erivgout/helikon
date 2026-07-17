package dev.helikon.client.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatHistory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsOptedInEntriesPerServerAndMaintainsABackup() {
        ChatHistory module = enabled(true);
        ChatHistoryManager source = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        long now = System.currentTimeMillis();
        source.activate(module, "Example.Org:25565");
        source.record(module, new ChatHistoryEntry(now - 10L, ChatHistoryEntry.Direction.INCOMING,
                "Alice_1", "Hello"), now);
        source.saveIfNeeded();
        source.record(module, new ChatHistoryEntry(now, ChatHistoryEntry.Direction.OUTGOING, "", "Draft"), now);
        source.saveIfNeeded();

        Path historyPath = source.pathForScope("example.org:25565");
        assertTrue(Files.exists(historyPath));
        assertTrue(Files.exists(historyPath.resolveSibling(historyPath.getFileName() + ".bak")));

        ChatHistoryManager loaded = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        loaded.activate(enabled(true), "example.org:25565");
        assertEquals(List.of(
                new ChatHistoryEntry(now, ChatHistoryEntry.Direction.OUTGOING, "", "Draft"),
                new ChatHistoryEntry(now - 10L, ChatHistoryEntry.Direction.INCOMING, "Alice_1", "Hello")
        ), loaded.entries());
    }

    @Test
    void leavesLoggingDisabledByDefaultAndPrunesExpiredEntriesInMemory() {
        ChatHistory module = enabled(false);
        numberSetting(module, "retention_days").set(1.0D);
        ChatHistoryManager manager = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        long now = System.currentTimeMillis();
        manager.activate(module, "example.org");
        manager.record(module, new ChatHistoryEntry(now - 2L * 24L * 60L * 60L * 1_000L,
                ChatHistoryEntry.Direction.INCOMING, "Alice_1", "expired"), now);

        assertTrue(manager.entries().isEmpty());
        assertFalse(Files.exists(manager.pathForScope("example.org")));
    }

    @Test
    void recoversAnInvalidPerServerFileWithoutLoadingIt() throws IOException {
        ChatHistory module = enabled(true);
        ChatHistoryManager manager = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        Path historyPath = manager.pathForScope("example.org");
        Files.createDirectories(historyPath.getParent());
        Files.writeString(historyPath, "{not json");

        manager.activate(module, "example.org");

        assertTrue(manager.entries().isEmpty());
        try (var files = Files.list(historyPath.getParent())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().contains(".corrupt-")));
        }
    }

    @Test
    void rejectsUnsafeServerScopesAndInvalidPlayerNames() {
        ChatHistoryManager manager = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        assertThrows(IllegalArgumentException.class, () -> manager.pathForScope("bad/server"));
        assertFalse(new ChatHistoryEntry(1L, ChatHistoryEntry.Direction.INCOMING, "system-message", "hello")
                .canCopyPlayerName());
    }

    private static ChatHistory enabled(boolean persistentLogging) {
        ChatHistory module = new ChatHistory();
        booleanSetting(module, "persistent_logging").set(persistentLogging);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(ChatHistory module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(ChatHistory module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
