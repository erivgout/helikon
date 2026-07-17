package dev.helikon.client.command;

/** Thin user-requested port for reopening one locally retained sent-chat draft. */
@FunctionalInterface
public interface ChatInputReopener {
    void reopen(String text);
}
