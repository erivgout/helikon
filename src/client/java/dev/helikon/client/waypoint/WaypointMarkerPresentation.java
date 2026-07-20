package dev.helikon.client.waypoint;

import java.util.Locale;
import java.util.Objects;

/** Minecraft-free Lunar-style label and distance-scale policy for world waypoint markers. */
public final class WaypointMarkerPresentation {
    private static final float MINIMUM_SCALE = 0.85F;
    private static final float MAXIMUM_SCALE = 1.8F;
    private static final float MAXIMUM_SCREEN_HEIGHT_FRACTION = 0.05F;
    private static final int DISTANCE_COMPENSATION_START = 8;
    private static final float SCALE_PER_DISTANT_BLOCK = 0.003F;

    private WaypointMarkerPresentation() {
    }

    public static Marker marker(WaypointNavigation.LocatedWaypoint located) {
        Objects.requireNonNull(located, "located");
        Waypoint waypoint = located.waypoint();
        int distance = Math.max(0, located.distance());
        return new Marker(icon(waypoint), waypoint.name() + " [" + distance + "m]", textScale(distance));
    }

    static float textScale(int distance) {
        if (distance < 0) {
            throw new IllegalArgumentException("Waypoint distance must not be negative");
        }
        int compensatedDistance = Math.max(0, distance - DISTANCE_COMPENSATION_START);
        return Math.clamp(MINIMUM_SCALE + compensatedDistance * SCALE_PER_DISTANT_BLOCK,
                MINIMUM_SCALE, MAXIMUM_SCALE);
    }

    /**
     * Caps the complete projected label panel, including its padding, to five
     * percent of the current GUI height at configured scale {@code 1.0}.
     */
    public static float screenLimitedScale(
            float adaptiveScale, float configuredScale, int labelHeight, int screenHeight
    ) {
        if (!Float.isFinite(adaptiveScale) || adaptiveScale <= 0.0F
                || !Float.isFinite(configuredScale) || configuredScale <= 0.0F
                || labelHeight <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("Waypoint label dimensions and scale must be positive");
        }
        float maximumScale = screenHeight * MAXIMUM_SCREEN_HEIGHT_FRACTION / labelHeight;
        return configuredScale * Math.min(adaptiveScale, maximumScale);
    }

    /** Raises the projected label onto the visible beam instead of leaving it at distant ground level. */
    public static double labelY(double waypointY, double cameraY, int distance) {
        if (!Double.isFinite(waypointY) || !Double.isFinite(cameraY) || distance < 0) {
            throw new IllegalArgumentException("Waypoint label anchor inputs must be valid");
        }
        double beamLift = Math.clamp(distance * 0.04D, 2.0D, 24.0D);
        return Math.max(waypointY + 2.0D, cameraY + beamLift);
    }

    private static String icon(Waypoint waypoint) {
        if (waypoint.icon().equals("death")) {
            return "X";
        }
        String name = waypoint.name().trim();
        return name.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    public record Marker(String icon, String label, float scale) {
        public Marker {
            icon = require(icon, "icon");
            label = require(label, "label");
            if (!Float.isFinite(scale) || scale <= 0.0F) {
                throw new IllegalArgumentException("Waypoint marker scale must be positive and finite");
            }
        }

        private static String require(String value, String field) {
            String result = Objects.requireNonNull(value, field).trim();
            if (result.isEmpty()) {
                throw new IllegalArgumentException(field + " must not be blank");
            }
            return result;
        }
    }
}
