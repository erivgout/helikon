package dev.helikon.client.command;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandDispatcherTest {
    private static HelikonCommand namedCommand(String name, Runnable action) {
        return new HelikonCommand() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String usage() {
                return CommandDispatcher.PREFIX + name;
            }

            @Override
            public String description() {
                return "Test command.";
            }

            @Override
            public void execute(List<String> arguments, CommandFeedback feedback) {
                action.run();
            }
        };
    }

    @Test
    void plainChatMessagesAreNotCommands() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        RecordingFeedback feedback = new RecordingFeedback();

        assertFalse(dispatcher.isCommand("hello world"));
        assertFalse(dispatcher.dispatch("hello world", feedback));
        assertTrue(feedback.all().isEmpty());
    }

    @Test
    void prefixedMessagesAreAlwaysHandledLocally() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        RecordingFeedback feedback = new RecordingFeedback();

        assertTrue(dispatcher.dispatch(".definitely_not_registered", feedback));
        assertEquals(1, feedback.errors.size());
        assertTrue(feedback.errors.get(0).contains("Unknown command"));
    }

    @Test
    void barePrefixReportsMissingCommandName() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        RecordingFeedback feedback = new RecordingFeedback();

        assertTrue(dispatcher.dispatch(".", feedback));
        assertTrue(feedback.errors.get(0).contains("Missing command name"));
    }

    @Test
    void commandNamesAreCaseInsensitive() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        boolean[] ran = {false};
        dispatcher.register(namedCommand("toggle", () -> ran[0] = true));

        assertTrue(dispatcher.dispatch(".TOGGLE", new RecordingFeedback()));
        assertTrue(ran[0]);
    }

    @Test
    void argumentsAreSplitOnWhitespace() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        AtomicReference<List<String>> seen = new AtomicReference<>();
        dispatcher.register(new HelikonCommand() {
            @Override
            public String name() {
                return "echo";
            }

            @Override
            public String usage() {
                return ".echo";
            }

            @Override
            public String description() {
                return "Test command.";
            }

            @Override
            public void execute(List<String> arguments, CommandFeedback feedback) {
                seen.set(arguments);
            }
        });

        dispatcher.dispatch(".echo  one   two ", new RecordingFeedback());
        assertEquals(List.of("one", "two"), seen.get());
    }

    @Test
    void duplicateCommandNamesAreRejected() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(namedCommand("gui", () -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> dispatcher.register(namedCommand("gui", () -> {
        })));
    }

    @Test
    void commandExceptionsAreReportedNotThrown() {
        CommandDispatcher dispatcher = new CommandDispatcher();
        dispatcher.register(namedCommand("boom", () -> {
            throw new IllegalStateException("expected failure");
        }));
        RecordingFeedback feedback = new RecordingFeedback();

        assertTrue(dispatcher.dispatch(".boom", feedback));
        assertEquals(1, feedback.errors.size());
        assertTrue(feedback.errors.get(0).contains("expected failure"));
    }
}
