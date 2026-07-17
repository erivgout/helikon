package dev.helikon.client.command;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.BetterChat;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterChatCommandTest {
    @Test
    void keepsSearchAndCopyLocalAndRequiresTheModuleToBeEnabled() {
        BetterChat betterChat = new BetterChat();
        List<String> history = List.of("Newest hello", "Older note", "hello again");
        List<String> clipboard = new ArrayList<>();
        BetterChatCommand command = new BetterChatCommand(betterChat, () -> history, clipboard::add);
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of("search", "hello"), feedback);
        assertTrue(feedback.errors.getFirst().contains("disabled"));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(betterChat);
        registry.setEnabled(betterChat, true);

        command.execute(List.of("search", "hello"), feedback);
        assertTrue(feedback.infos.stream().anyMatch(line -> line.equals("Newest hello")));
        assertTrue(feedback.infos.stream().anyMatch(line -> line.equals("hello again")));

        command.execute(List.of("copy", "2"), feedback);
        assertEquals(List.of("Older note"), clipboard);
        assertTrue(feedback.infos.getLast().contains("Copied local chat line #2"));
    }

    @Test
    void boundsHistoryDisplayAndRejectsOutOfRangeCopyIndexes() {
        BetterChat betterChat = enabled();
        BetterChatCommand command = new BetterChatCommand(betterChat, () -> List.of("one", "two"), ignored -> { });
        RecordingFeedback feedback = new RecordingFeedback();

        command.execute(List.of("history", "1"), feedback);
        assertTrue(feedback.infos.contains("#1 one"));
        assertTrue(feedback.infos.stream().noneMatch(line -> line.equals("#2 two")));

        command.execute(List.of("copy", "3"), feedback);
        assertTrue(feedback.errors.getLast().contains("retained local line"));
    }

    private static BetterChat enabled() {
        BetterChat betterChat = new BetterChat();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(betterChat);
        registry.setEnabled(betterChat, true);
        return betterChat;
    }
}
