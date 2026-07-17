package dev.helikon.client.command;

import dev.helikon.client.chat.ChatHistoryEntry;
import dev.helikon.client.chat.ChatHistoryManager;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatHistory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatHistoryCommandTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void keepsSearchCopyPlayerAndDraftReopenLocal() {
        ChatHistory module = enabled();
        ChatHistoryManager history = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        long now = System.currentTimeMillis();
        history.activate(module, "example.org");
        history.record(module, new ChatHistoryEntry(now - 1L, ChatHistoryEntry.Direction.INCOMING,
                "Alice_1", "Need a hello"), now);
        history.record(module, new ChatHistoryEntry(now, ChatHistoryEntry.Direction.OUTGOING, "", "my draft"), now);
        List<String> clipboard = new ArrayList<>();
        List<String> reopened = new ArrayList<>();
        AtomicReference<Runnable> pendingReopen = new AtomicReference<>();
        ChatHistoryCommand command = new ChatHistoryCommand(module, history, clipboard::add,
                new ScheduledChatInputReopener(reopened::add, pendingReopen::set));
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of("search", "hello"), feedback);
        command.execute(List.of("player", "2"), feedback);
        command.execute(List.of("copy", "1"), feedback);
        command.execute(List.of("reopen", "1"), feedback);
        command.execute(List.of("list", "1"), feedback);

        assertEquals(List.of("Alice_1", "my draft"), clipboard);
        assertTrue(reopened.isEmpty());
        pendingReopen.get().run();
        assertEquals(List.of("my draft"), reopened);
        assertTrue(feedback.infos.contains("Need a hello"));
        assertTrue(feedback.infos.contains("#1 my draft"));
    }

    @Test
    void rejectsActionsWhenDisabledOrNotApplicable() {
        ChatHistory module = new ChatHistory();
        ChatHistoryManager history = new ChatHistoryManager(temporaryDirectory.resolve("helikon"));
        ChatHistoryCommand command = new ChatHistoryCommand(module, history, ignored -> { }, ignored -> { });
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of("list"), feedback);
        assertTrue(feedback.errors.getFirst().contains("disabled"));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        history.activate(module, "example.org");
        long now = System.currentTimeMillis();
        history.record(module, new ChatHistoryEntry(now, ChatHistoryEntry.Direction.INCOMING, "Alice_1", "hello"), now);
        command.execute(List.of("reopen", "1"), feedback);
        command.execute(List.of("player", "0"), feedback);

        assertTrue(feedback.errors.stream().anyMatch(message -> message.contains("Only a locally retained sent")));
        assertTrue(feedback.errors.stream().anyMatch(message -> message.contains("History index")));
    }

    private static ChatHistory enabled() {
        ChatHistory module = new ChatHistory();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
