package dev.helikon.client.hud;

import java.util.List;
import java.util.Locale;

/** Minecraft-free formatting for the live Coordinates HUD module. */
public final class CoordinateReadout {
    private CoordinateReadout() {
    }

    public static List<String> lines(double x, double y, double z, String dimension,
                                     boolean decimals, boolean showDimension) {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return List.of("XYZ --");
        }
        String coordinates = decimals
                ? String.format(Locale.ROOT, "XYZ %.1f, %.1f, %.1f", x, y, z)
                : String.format(Locale.ROOT, "XYZ %d, %d, %d", floor(x), floor(y), floor(z));
        if (!showDimension) {
            return List.of(coordinates);
        }
        return List.of(coordinates, "Dimension " + TelemetryText.titleCaseIdentifier(dimension));
    }

    private static long floor(double value) {
        return (long) Math.floor(value);
    }
}
