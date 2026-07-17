package dev.helikon.client.render;

import java.util.Objects;

/** Incremental bounded cube scan cursor, deliberately independent of Minecraft world types. */
public final class BlockEspScanCursor {
    private Region activeRegion;
    private int x;
    private int y;
    private int z;

    /** Returns one local scan coordinate, restarting automatically after a complete pass. */
    public Position next(Region region) {
        Region requested = Objects.requireNonNull(region, "region");
        if (!requested.equals(activeRegion)) {
            reset(requested);
        }

        Position position = new Position(x, y, z);
        advance();
        return position;
    }

    public void clear() {
        activeRegion = null;
    }

    /** True before the first coordinate of a new full pass, including immediately after clear. */
    public boolean isAtPassBoundary() {
        return activeRegion == null || (x == activeRegion.minimumX()
                && y == activeRegion.minimumY() && z == activeRegion.minimumZ());
    }

    private void reset(Region region) {
        activeRegion = region;
        x = region.minimumX();
        y = region.minimumY();
        z = region.minimumZ();
    }

    private void advance() {
        if (++x <= activeRegion.maximumX()) {
            return;
        }
        x = activeRegion.minimumX();
        if (++z <= activeRegion.maximumZ()) {
            return;
        }
        z = activeRegion.minimumZ();
        if (++y <= activeRegion.maximumY()) {
            return;
        }
        reset(activeRegion);
    }

    /** Inclusive horizontal and vertical limits around the current local player block position. */
    public record Region(int centerX, int centerY, int centerZ, int horizontalRadius, int verticalRadius) {
        public Region {
            if (horizontalRadius < 0 || verticalRadius < 0) {
                throw new IllegalArgumentException("Scan radii must not be negative");
            }
        }

        public int minimumX() { return centerX - horizontalRadius; }

        public int maximumX() { return centerX + horizontalRadius; }

        public int minimumY() { return centerY - verticalRadius; }

        public int maximumY() { return centerY + verticalRadius; }

        public int minimumZ() { return centerZ - horizontalRadius; }

        public int maximumZ() { return centerZ + horizontalRadius; }
    }

    public record Position(int x, int y, int z) {
    }
}
