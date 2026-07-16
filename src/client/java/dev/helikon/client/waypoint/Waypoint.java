package dev.helikon.client.waypoint;

import java.util.Objects;
import java.util.regex.Pattern;

/** Immutable, local-only waypoint data persisted by {@link WaypointManager}. */
public record Waypoint(
        String name,
        int x,
        int y,
        int z,
        WaypointContext context,
        int color,
        String icon,
        boolean enabled,
        long createdAtEpochMillis
) {
    public static final int DEFAULT_COLOR = 0xFF55FFFF;
    public static final String NO_ICON = "";
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9][A-Za-z0-9 _-]{0,31}");
    private static final Pattern ICON_PATTERN = Pattern.compile("[a-z0-9_:-]{0,64}");
    private static final int MAX_COORDINATE = 30_000_000;

    public Waypoint {
        name = requireName(name);
        requireCoordinate(x, "x");
        requireCoordinate(y, "y");
        requireCoordinate(z, "z");
        context = Objects.requireNonNull(context, "context");
        icon = requireIcon(icon);
        if (createdAtEpochMillis < 0) {
            throw new IllegalArgumentException("Waypoint creation time must not be negative");
        }
    }

    public static String requireName(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (!NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Waypoint names must use 1-32 letters, digits, spaces, '_' or '-' and start with a letter or digit");
        }
        return normalized;
    }

    public static String requireIcon(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim().toLowerCase(java.util.Locale.ROOT);
        if (!ICON_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Waypoint icons must use lowercase letters, digits, '_', ':' or '-'");
        }
        return normalized;
    }

    public static String normalizedName(String value) {
        return requireName(value).toLowerCase(java.util.Locale.ROOT);
    }

    private static void requireCoordinate(int coordinate, String axis) {
        if (Math.abs((long) coordinate) > MAX_COORDINATE) {
            throw new IllegalArgumentException("Waypoint " + axis + " coordinate is outside the world border");
        }
    }
}
