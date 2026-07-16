package dev.helikon.client.macro;

/** Thin boundary for the three explicit macro action effects. */
public interface MacroActionExecutor {
    void executeLocalCommand(String command);

    void sendChat(String message);

    void sendMinecraftCommand(String command);
}
