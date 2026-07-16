package dev.helikon.client.macro;

import java.util.Locale;
import java.util.Objects;

/** One validated, non-scriptable macro action. */
public record MacroAction(MacroActionType type, String text, int delayTicks) {
    public static final int MAX_TEXT_LENGTH = 256;
    public static final int MAX_DELAY_TICKS = 12_000;

    public MacroAction {
        type = Objects.requireNonNull(type, "type");
        text = normalizeText(type, text);
        if (type == MacroActionType.DELAY) {
            if (delayTicks < 1 || delayTicks > MAX_DELAY_TICKS) {
                throw new IllegalArgumentException("Macro delay must be between 1 and " + MAX_DELAY_TICKS + " ticks");
            }
        } else if (delayTicks != 0) {
            throw new IllegalArgumentException("Only delay actions may have delay ticks");
        }
    }

    public static MacroAction local(String command) {
        return new MacroAction(MacroActionType.LOCAL, command, 0);
    }

    public static MacroAction chat(String message) {
        return new MacroAction(MacroActionType.CHAT, message, 0);
    }

    public static MacroAction command(String command) {
        return new MacroAction(MacroActionType.COMMAND, command, 0);
    }

    public static MacroAction delay(int ticks) {
        return new MacroAction(MacroActionType.DELAY, "", ticks);
    }

    private static String normalizeText(MacroActionType type, String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (type == MacroActionType.DELAY) {
            if (!normalized.isEmpty()) {
                throw new IllegalArgumentException("Delay actions cannot contain text");
            }
            return "";
        }
        if (normalized.isEmpty() || normalized.length() > MAX_TEXT_LENGTH || hasControlCharacter(normalized)) {
            throw new IllegalArgumentException("Macro action text must contain 1-" + MAX_TEXT_LENGTH + " printable characters");
        }
        if (type == MacroActionType.LOCAL) {
            if (!normalized.startsWith(".")) {
                throw new IllegalArgumentException("Local macro actions must start with '.'");
            }
            if (normalized.toLowerCase(Locale.ROOT).startsWith(".macro")) {
                throw new IllegalArgumentException("Macros cannot start or modify another macro");
            }
        } else if (type == MacroActionType.CHAT) {
            if (normalized.startsWith(".") || normalized.startsWith("/")) {
                throw new IllegalArgumentException("Chat macro actions cannot begin with '.' or '/'");
            }
        } else if (type == MacroActionType.COMMAND && (normalized.startsWith(".") || normalized.startsWith("/"))) {
            throw new IllegalArgumentException("Minecraft macro commands must omit '.' and '/'");
        }
        return normalized;
    }

    private static boolean hasControlCharacter(String text) {
        return text.chars().anyMatch(character -> Character.isISOControl(character));
    }
}
