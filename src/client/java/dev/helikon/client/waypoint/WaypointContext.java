package dev.helikon.client.waypoint;

import java.util.Objects;
import java.util.regex.Pattern;

/** The local server/world and dimension to which a waypoint belongs. */
public record WaypointContext(String scope, String dimension) {
    private static final Pattern DIMENSION_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Pattern SCOPE_PATTERN = Pattern.compile("(?:server|world):[^\\p{Cntrl}]{1,192}");

    public WaypointContext {
        scope = requireScope(scope);
        dimension = requireDimension(dimension);
    }

    public static String requireScope(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (!SCOPE_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Waypoint scope must identify a local server or world");
        }
        return normalized;
    }

    public static String requireDimension(String value) {
        String normalized = Objects.requireNonNullElse(value, "").trim();
        if (!DIMENSION_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Waypoint dimension must be a valid identifier");
        }
        return normalized;
    }
}
