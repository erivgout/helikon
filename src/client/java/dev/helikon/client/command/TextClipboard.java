package dev.helikon.client.command;

/** Narrow output port for an explicit user-requested local clipboard copy. */
@FunctionalInterface
public interface TextClipboard {
    void copy(String text);
}
