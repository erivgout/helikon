package dev.helikon.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Bounded sampling and refresh policy for the optional Radar terrain minimap. */
public final class RadarMinimapSampling {
    public static final int CELL_SIZE = 1;
    public static final long REFRESH_INTERVAL_TICKS = 20L;
    public static final int MOVEMENT_REFRESH_BLOCKS = 4;
    public static final float YAW_STEP_DEGREES = 15.0F;

    private RadarMinimapSampling() {
    }

    public static List<Cell> cells(int radius, RadarProjection.Shape shape) {
        if (radius <= 0) {
            throw new IllegalArgumentException("Minimap radius must be positive");
        }
        Objects.requireNonNull(shape, "shape");
        List<Cell> cells = new ArrayList<>();
        for (int screenY = -radius; screenY < radius; screenY += CELL_SIZE) {
            for (int screenX = -radius; screenX < radius; screenX += CELL_SIZE) {
                double sampleX = screenX + CELL_SIZE * 0.5D;
                double sampleY = screenY + CELL_SIZE * 0.5D;
                if (shape == RadarProjection.Shape.CIRCLE
                        && sampleX * sampleX + sampleY * sampleY > radius * radius) {
                    continue;
                }
                cells.add(new Cell(screenX, screenY));
            }
        }
        return List.copyOf(cells);
    }

    public static long refreshBucket(long gameTime) {
        return Math.floorDiv(gameTime, REFRESH_INTERVAL_TICKS);
    }

    public static int yawBucket(float yawDegrees, boolean rotating) {
        return rotating ? Math.round(yawDegrees / YAW_STEP_DEGREES) : 0;
    }

    public static boolean movedFarEnough(int previousX, int previousZ, int currentX, int currentZ) {
        return Math.abs(currentX - previousX) >= MOVEMENT_REFRESH_BLOCKS
                || Math.abs(currentZ - previousZ) >= MOVEMENT_REFRESH_BLOCKS;
    }

    public record Cell(int x, int y) {
    }
}
