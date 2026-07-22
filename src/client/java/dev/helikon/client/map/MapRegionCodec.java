package dev.helikon.client.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Objects;
import java.util.function.LongSupplier;
import java.util.zip.CRC32;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** Versioned, bounded GZIP codec and atomic recovery path for map regions. */
public final class MapRegionCodec {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAGIC = 0x484B4D50;

    private final LongSupplier clock;

    public MapRegionCodec() {
        this(System::currentTimeMillis);
    }

    MapRegionCodec(LongSupplier clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public byte[] encode(MapRegion.Snapshot snapshot) throws IOException {
        Objects.requireNonNull(snapshot, "snapshot");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        CRC32 checksum = new CRC32();
        try (DataOutputStream data = new DataOutputStream(new GZIPOutputStream(output))) {
            data.writeInt(MAGIC);
            data.writeInt(SCHEMA_VERSION);
            data.writeInt(snapshot.regionX());
            data.writeInt(snapshot.regionZ());
            data.writeInt(MapRegion.PIXEL_COUNT);
            int[] pixels = snapshot.copyPixels();
            for (int pixel : pixels) {
                data.writeInt(pixel);
                update(checksum, pixel);
            }
            data.writeLong(checksum.getValue());
        }
        return output.toByteArray();
    }

    public MapRegion.Snapshot decode(byte[] encoded, int expectedRegionX, int expectedRegionZ) throws IOException {
        Objects.requireNonNull(encoded, "encoded");
        CRC32 checksum = new CRC32();
        try (DataInputStream data = new DataInputStream(
                new GZIPInputStream(new ByteArrayInputStream(encoded)))) {
            if (data.readInt() != MAGIC) {
                throw new MapFormatException("Invalid map region magic");
            }
            int version = data.readInt();
            if (version > SCHEMA_VERSION) {
                throw new UnsupportedVersionException(version);
            }
            if (version != SCHEMA_VERSION) {
                throw new MapFormatException("Unsupported map region schema " + version);
            }
            int regionX = data.readInt();
            int regionZ = data.readInt();
            if (regionX != expectedRegionX || regionZ != expectedRegionZ) {
                throw new MapFormatException("Map region coordinates do not match the file name");
            }
            if (data.readInt() != MapRegion.PIXEL_COUNT) {
                throw new MapFormatException("Invalid map region pixel count");
            }
            int[] pixels = new int[MapRegion.PIXEL_COUNT];
            for (int index = 0; index < pixels.length; index++) {
                int pixel = data.readInt();
                if (pixel != 0 && (pixel & 0xFF000000) != 0xFF000000) {
                    throw new MapFormatException("Map region contains a non-opaque discovered pixel");
                }
                pixels[index] = pixel;
                update(checksum, pixel);
            }
            long storedChecksum = data.readLong();
            if (storedChecksum != checksum.getValue()) {
                throw new MapFormatException("Map region checksum mismatch");
            }
            if (data.read() != -1) {
                throw new MapFormatException("Map region contains trailing data");
            }
            return new MapRegion.Snapshot(regionX, regionZ, pixels, 0L);
        } catch (UnsupportedVersionException exception) {
            throw exception;
        } catch (EOFException exception) {
            throw new MapFormatException("Truncated map region", exception);
        } catch (MapFormatException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new MapFormatException("Unable to decode map region", exception);
        }
    }

    public MapRegion.Snapshot readWithRecovery(Path primary, int regionX, int regionZ) throws IOException {
        Objects.requireNonNull(primary, "primary");
        if (Files.notExists(primary)) {
            Path backup = backupPath(primary);
            if (Files.exists(backup)) {
                return decode(Files.readAllBytes(backup), regionX, regionZ);
            }
            return new MapRegion(regionX, regionZ).snapshot();
        }
        try {
            return decode(Files.readAllBytes(primary), regionX, regionZ);
        } catch (UnsupportedVersionException exception) {
            throw exception;
        } catch (IOException exception) {
            preserveMalformed(primary);
            Path backup = backupPath(primary);
            if (Files.exists(backup)) {
                return decode(Files.readAllBytes(backup), regionX, regionZ);
            }
            return new MapRegion(regionX, regionZ).snapshot();
        }
    }

    public void writeAtomic(Path destination, byte[] encoded) throws IOException {
        Objects.requireNonNull(destination, "destination");
        Objects.requireNonNull(encoded, "encoded");
        Files.createDirectories(destination.getParent());
        Path temporary = Files.createTempFile(destination.getParent(), "map-region-", ".hmap.tmp");
        try {
            Files.write(temporary, encoded);
            if (Files.exists(destination)) {
                Files.copy(destination, backupPath(destination), StandardCopyOption.REPLACE_EXISTING);
            }
            moveAtomically(temporary, destination);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static Path backupPath(Path primary) {
        return primary.resolveSibling(primary.getFileName() + ".bak");
    }

    private void preserveMalformed(Path primary) throws IOException {
        String name = primary.getFileName().toString();
        String stem = name.endsWith(".hmap") ? name.substring(0, name.length() - 5) : name;
        Path corrupt = primary.resolveSibling(stem + ".corrupt-" + Instant.ofEpochMilli(clock.getAsLong()).toEpochMilli()
                + ".hmap");
        Files.move(primary, corrupt, StandardCopyOption.REPLACE_EXISTING);
    }

    private static void update(CRC32 checksum, int value) {
        checksum.update(value >>> 24 & 0xFF);
        checksum.update(value >>> 16 & 0xFF);
        checksum.update(value >>> 8 & 0xFF);
        checksum.update(value & 0xFF);
    }

    private static void moveAtomically(Path source, Path destination) throws IOException {
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static class MapFormatException extends IOException {
        public MapFormatException(String message) {
            super(message);
        }

        public MapFormatException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class UnsupportedVersionException extends MapFormatException {
        private final int version;

        public UnsupportedVersionException(int version) {
            super("Map region schema " + version + " is newer than this client supports");
            this.version = version;
        }

        public int version() {
            return version;
        }
    }
}
