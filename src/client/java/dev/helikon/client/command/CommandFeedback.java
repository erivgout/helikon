package dev.helikon.client.command;

/** Where a command's local responses go: the chat HUD in game, a recorder in tests. */
public interface CommandFeedback {
    void info(String message);

    void error(String message);
}
