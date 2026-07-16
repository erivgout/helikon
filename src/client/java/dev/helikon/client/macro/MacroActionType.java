package dev.helikon.client.macro;

import java.util.Locale;

/** The deliberately closed set of actions a local macro may perform. */
public enum MacroActionType {
    LOCAL,
    CHAT,
    COMMAND,
    DELAY;

    public static MacroActionType parse(String value) {
        try {
            return valueOf(value == null ? "" : value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Macro action type must be local, chat, command, or delay");
        }
    }
}
