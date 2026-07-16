package dev.helikon.client.command;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses and executes {@code .}-prefixed local chat commands. A message that
 * starts with the prefix is always handled locally — including unknown
 * commands — so command attempts are never sent to the server. Command
 * failures are reported as feedback and logged, never thrown at the caller.
 */
public final class CommandDispatcher {
    public static final String PREFIX = ".";

    private static final Logger LOGGER = Logger.getLogger(CommandDispatcher.class.getName());

    private final Map<String, HelikonCommand> commands = new LinkedHashMap<>();

    public void register(HelikonCommand command) {
        HelikonCommand nonNullCommand = Objects.requireNonNull(command, "command");
        String name = nonNullCommand.name().toLowerCase(Locale.ROOT);
        if (commands.containsKey(name)) {
            throw new IllegalArgumentException("A command is already registered with name '" + name + "'");
        }
        commands.put(name, nonNullCommand);
    }

    public Collection<HelikonCommand> commands() {
        return List.copyOf(commands.values());
    }

    public Optional<HelikonCommand> find(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(commands.get(name.toLowerCase(Locale.ROOT)));
    }

    /** Whether the raw chat message is a command attempt this dispatcher owns. */
    public boolean isCommand(String rawMessage) {
        return rawMessage != null && rawMessage.startsWith(PREFIX);
    }

    /**
     * Executes the message as a command. Returns {@code true} when the message
     * was a command attempt and must not be sent to the server.
     */
    public boolean dispatch(String rawMessage, CommandFeedback feedback) {
        Objects.requireNonNull(feedback, "feedback");
        if (!isCommand(rawMessage)) {
            return false;
        }

        List<String> tokens = List.of(rawMessage.substring(PREFIX.length()).trim().split("\\s+"));
        String name = tokens.get(0).toLowerCase(Locale.ROOT);
        if (name.isEmpty()) {
            feedback.error("Missing command name. Use " + PREFIX + "help to list commands.");
            return true;
        }

        HelikonCommand command = commands.get(name);
        if (command == null) {
            feedback.error("Unknown command '" + PREFIX + name + "'. Use " + PREFIX + "help to list commands.");
            return true;
        }

        try {
            command.execute(tokens.subList(1, tokens.size()), feedback);
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "Command '" + PREFIX + name + "' failed", exception);
            feedback.error("Command failed: " + exception.getMessage());
        }
        return true;
    }
}
