package dev.helikon.client.module.render;

import java.util.List;

/** Minecraft-free geometry for BetterCrosshair's four center-aligned arms. */
public final class CrosshairGeometry {
    private CrosshairGeometry() {
    }

    public static List<Rect> arms(int centerX, int centerY, int size, int gap, int thickness, int movementGap) {
        if (size < 1 || gap < 0 || thickness < 1 || movementGap < 0) {
            throw new IllegalArgumentException("Crosshair geometry values are out of range");
        }
        int offset = gap + movementGap;
        int halfThickness = thickness / 2;
        return List.of(
                new Rect(centerX - offset - size, centerY - halfThickness, size, thickness),
                new Rect(centerX + offset + 1, centerY - halfThickness, size, thickness),
                new Rect(centerX - halfThickness, centerY - offset - size, thickness, size),
                new Rect(centerX - halfThickness, centerY + offset + 1, thickness, size)
        );
    }

    public record Rect(int x, int y, int width, int height) {
    }
}
