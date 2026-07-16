package dev.helikon.client.command;

import java.util.List;
import java.util.Objects;

/** Lists every registered command with its usage and description. */
public final class HelpCommand implements HelikonCommand {
    private final CommandDispatcher dispatcher;

    public HelpCommand(CommandDispatcher dispatcher) {
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher");
    }

    @Override
    public String name() {
        return "help";
    }

    @Override
    public String usage() {
        return CommandDispatcher.PREFIX + "help";
    }

    @Override
    public String description() {
        return "Lists all local Helikon commands.";
    }

    @Override
    public void execute(List<String> arguments, CommandFeedback feedback) {
        feedback.info("Local commands (never sent to the server):");
        for (HelikonCommand command : dispatcher.commands()) {
            feedback.info(command.usage() + " — " + command.description());
        }
    }
}
