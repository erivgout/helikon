package dev.helikon.client.map;

import java.util.Arrays;
import java.util.Objects;

/** Mutable worker-owned 256x256 persistent terrain region. */
public final class MapRegion {
    public static final int SIZE = 256;
    public static final int CHUNKS_PER_SIDE = SIZE / MapChunkSnapshot.SIZE;
    public static final int PIXEL_COUNT = SIZE * SIZE;

    private final int regionX;
    private final int regionZ;
    private final int[] pixels;
    private long revision;

    public MapRegion(int regionX, int regionZ) {
        this(regionX, regionZ, new int[PIXEL_COUNT], 0L);
    }

    public MapRegion(int regionX, int regionZ, int[] pixels, long revision) {
        if (Objects.requireNonNull(pixels, "pixels").length != PIXEL_COUNT) {
            throw new IllegalArgumentException("Map regions require exactly " + PIXEL_COUNT + " pixels");
        }
        if (revision < 0L) {
            throw new IllegalArgumentException("Map region revision must be non-negative");
        }
        this.regionX = regionX;
        this.regionZ = regionZ;
        this.pixels = Arrays.copyOf(pixels, pixels.length);
        this.revision = revision;
    }

    public static int regionCoordinateForBlock(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, SIZE);
    }

    public static int localCoordinateForBlock(int blockCoordinate) {
        return Math.floorMod(blockCoordinate, SIZE);
    }

    public static int regionCoordinateForChunk(int chunkCoordinate) {
        return Math.floorDiv(chunkCoordinate, CHUNKS_PER_SIDE);
    }

    public int regionX() {
        return regionX;
    }

    public int regionZ() {
        return regionZ;
    }

    public long revision() {
        return revision;
    }

    /** Applies opaque discovered pixels without erasing earlier data for absent samples. */
    public boolean apply(MapChunkSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (regionCoordinateForChunk(snapshot.chunkX()) != regionX
                || regionCoordinateForChunk(snapshot.chunkZ()) != regionZ) {
            throw new IllegalArgumentException("Chunk snapshot does not belong to this map region");
        }
        int blockStartX = snapshot.chunkX() * MapChunkSnapshot.SIZE;
        int blockStartZ = snapshot.chunkZ() * MapChunkSnapshot.SIZE;
        boolean changed = false;
        for (int localZ = 0; localZ < MapChunkSnapshot.SIZE; localZ++) {
            int regionLocalZ = localCoordinateForBlock(blockStartZ + localZ);
            for (int localX = 0; localX < MapChunkSnapshot.SIZE; localX++) {
                int color = snapshot.pixel(localX, localZ);
                if (color == 0) {
                    continue;
                }
                int regionLocalX = localCoordinateForBlock(blockStartX + localX);
                int index = regionLocalZ * SIZE + regionLocalX;
                if (pixels[index] != color) {
                    pixels[index] = color;
                    changed = true;
                }
            }
        }
        if (changed) {
            revision++;
        }
        return changed;
    }

    public Snapshot snapshot() {
        return new Snapshot(regionX, regionZ, pixels, revision);
    }

    /** Immutable defensive region view safe to publish to the render thread. */
    public static final class Snapshot {
        private final int regionX;
        private final int regionZ;
        private final int[] pixels;
        private final long revision;

        public Snapshot(int regionX, int regionZ, int[] pixels, long revision) {
            if (Objects.requireNonNull(pixels, "pixels").length != PIXEL_COUNT) {
                throw new IllegalArgumentException("Map region snapshots require exactly " + PIXEL_COUNT + " pixels");
            }
            if (revision < 0L) {
                throw new IllegalArgumentException("Map region revision must be non-negative");
            }
            this.regionX = regionX;
            this.regionZ = regionZ;
            this.pixels = Arrays.copyOf(pixels, pixels.length);
            this.revision = revision;
        }

        public int regionX() {
            return regionX;
        }

        public int regionZ() {
            return regionZ;
        }

        public long revision() {
            return revision;
        }

        public int pixel(int localX, int localZ) {
            if (localX < 0 || localX >= SIZE || localZ < 0 || localZ >= SIZE) {
                throw new IndexOutOfBoundsException("Region pixel is outside 0..255");
            }
            return pixels[localZ * SIZE + localX];
        }

        public int[] copyPixels() {
            return Arrays.copyOf(pixels, pixels.length);
        }
    }
}

