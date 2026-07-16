package dev.helikon.client.macro;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable local macro, optionally restricted to one normalized server address. */
public record Macro(String name, String serverAddress, List<MacroAction> actions) {
    public static final String GLOBAL = "";
    public static final int MAX_ACTIONS = 64;
    private static final Pattern NAME_PATTERN = Pattern.compile("[a-z0-9][a-z0-9_-]{0,31}");

    public Macro {
        name = normalizeName(name);
        serverAddress = normalizeServerAddress(serverAddress);
        actions = List.copyOf(Objects.requireNonNull(actions, "actions"));
        if (actions.size() > MAX_ACTIONS) {
            throw new IllegalArgumentException("Macros may contain at most " + MAX_ACTIONS + " actions");
        }
        actions.forEach(action -> Objects.requireNonNull(action, "action"));
    }

    public boolean isServerScoped() {
        return !serverAddress.isEmpty();
    }

    public static String normalizeName(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Macro names must use 1-32 lowercase letters, digits, '-' or '_' and start with a letter or digit");
        }
        return normalized;
    }

    public static String normalizeServerAddress(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            return GLOBAL;
        }
        if (normalized.length() > 255 || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Macro server address must be a non-control value no longer than 255 characters");
        }
        return normalized;
    }
}
