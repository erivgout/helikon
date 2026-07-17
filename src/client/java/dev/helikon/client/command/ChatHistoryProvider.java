package dev.helikon.client.command;

import java.util.List;

/** Narrow local-only port exposing the current client chat display, newest first. */
@FunctionalInterface
public interface ChatHistoryProvider {
    List<String> messages();
}
