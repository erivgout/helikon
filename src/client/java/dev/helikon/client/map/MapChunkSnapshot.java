package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;

import java.util.Arrays;
import java.util.Objects;

/** Immutable one-pixel-per-block terrain snapshot for one loaded chunk. */
public final class MapChunkSnapshot {
    public static final int SIZE = 16;
    public static final int PIXEL_COUNT = SIZE * SIZE;

    private final WaypointContext context;
    private final int chunkX;
    private final int chunkZ;
    private final int[] pixels;

    public MapChunkSnapshot(WaypointContext context, int chunkX, int chunkZ, int[] pixels) {
        this.context = Objects.requireNonNull(context, "context");
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        if (Objects.requireNonNull(pixels, "pixels").length != PIXEL_COUNT) {
            throw new IllegalArgumentException("Map chunk snapshots require exactly " + PIXEL_COUNT + " pixels");
        }
        this.pixels = Arrays.copyOf(pixels, pixels.length);
        for (int color : this.pixels) {
            if (color != 0 && (color & 0xFF000000) != 0xFF000000) {
                throw new IllegalArgumentException("Discovered map pixels must be opaque ARGB colors");
            }
        }
    }

    public WaypointContext context() {
        return context;
    }

    public int chunkX() {
        return chunkX;
    }

    public int chunkZ() {
        return chunkZ;
    }

    public int pixel(int localX, int localZ) {
        if (localX < 0 || localX >= SIZE || localZ < 0 || localZ >= SIZE) {
            throw new IndexOutOfBoundsException("Chunk pixel is outside 0..15");
        }
        return pixels[localZ * SIZE + localX];
    }

    int[] copyPixels() {
        return Arrays.copyOf(pixels, pixels.length);
    }
}

