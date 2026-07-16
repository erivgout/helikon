package dev.helikon.client.command;

import java.util.List;

/**
 * A local chat command. Commands run entirely on the client; nothing they do
 * is sent to the server.
 */
public interface HelikonCommand {
    /** Stable lowercase command name without the prefix, e.g. {@code toggle}. */
    String name();

    /** Usage line shown by {@code .help} and on argument errors, e.g. {@code .toggle <module>}. */
    String usage();

    String description();

    void execute(List<String> arguments, CommandFeedback feedback);
}
