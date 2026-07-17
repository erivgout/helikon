package dev.helikon.client.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandCompletionTest {
    private static final List<HelikonCommand> COMMANDS = List.of(command("help"), command("hide"), command("toggle"));

    @Test
    void completesAnUnambiguousLocalCommand() {
        CommandCompletion.Result result = CommandCompletion.complete(".to", 3, COMMANDS);

        assertTrue(result.changed());
        assertEquals(".toggle", result.value());
        assertEquals(7, result.cursor());
    }

    @Test
    void leavesAmbiguousCommandPrefixesUntouched() {
        CommandCompletion.Result result = CommandCompletion.complete(".h", 2, COMMANDS);

        assertFalse(result.changed());
        assertEquals(".h", result.value());
        assertEquals(List.of(), result.matches());
    }

    @Test
    void leavesServerMessagesAndArgumentsUntouched() {
        assertFalse(CommandCompletion.complete("/help", 5, COMMANDS).changed());
        assertFalse(CommandCompletion.complete(".toggle auto", 8, COMMANDS).changed());
    }

    private static HelikonCommand command(String name) {
        return new HelikonCommand() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public String usage() {
                return "." + name;
            }

            @Override
            public String description() {
                return name;
            }

            @Override
            public void execute(List<String> arguments, CommandFeedback feedback) {
            }
        };
    }
}
