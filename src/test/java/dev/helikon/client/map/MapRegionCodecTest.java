package dev.helikon.client.map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapRegionCodecTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsSparseAndFullRegions() throws IOException {
        MapRegionCodec codec = new MapRegionCodec();
        int[] pixels = new int[MapRegion.PIXEL_COUNT];
        pixels[0] = 0xFF010203;
        pixels[pixels.length - 1] = 0xFFFDFEFF;
        MapRegion.Snapshot decoded = codec.decode(codec.encode(
                new MapRegion.Snapshot(-2, 4, pixels, 9L)), -2, 4);

        assertEquals(-2, decoded.regionX());
        assertEquals(4, decoded.regionZ());
        assertEquals(0xFF010203, decoded.pixel(0, 0));
        assertEquals(0xFFFDFEFF, decoded.pixel(255, 255));
        assertEquals(0, decoded.pixel(10, 10));
    }

    @Test
    void rejectsWrongCoordinatesAndTruncation() throws IOException {
        MapRegionCodec codec = new MapRegionCodec();
        byte[] encoded = codec.encode(new MapRegion(1, 2).snapshot());
        assertThrows(MapRegionCodec.MapFormatException.class, () -> codec.decode(encoded, 2, 1));
        assertThrows(MapRegionCodec.MapFormatException.class,
                () -> codec.decode(java.util.Arrays.copyOf(encoded, encoded.length / 2), 1, 2));
    }

    @Test
    void rejectsNewerVersionsCrcDamageAndTrailingData() throws IOException {
        MapRegionCodec codec = new MapRegionCodec();
        byte[] encoded = codec.encode(new MapRegion(1, 2).snapshot());

        byte[] newer;
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(bytes);
             DataOutputStream output = new DataOutputStream(gzip)) {
            output.writeInt(0x484B4D50);
            output.writeInt(MapRegionCodec.SCHEMA_VERSION + 1);
            output.flush();
            gzip.finish();
            newer = bytes.toByteArray();
        }
        assertThrows(MapRegionCodec.UnsupportedVersionException.class, () -> codec.decode(newer, 1, 2));

        byte[] raw;
        try (GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(encoded))) {
            raw = gzip.readAllBytes();
        }
        raw[24] ^= 1;
        assertThrows(MapRegionCodec.MapFormatException.class,
                () -> codec.decode(gzip(raw), 1, 2));

        byte[] withTrailingData = java.util.Arrays.copyOf(raw, raw.length + 1);
        raw[24] ^= 1;
        System.arraycopy(raw, 0, withTrailingData, 0, raw.length);
        assertThrows(MapRegionCodec.MapFormatException.class,
                () -> codec.decode(gzip(withTrailingData), 1, 2));
    }

    @Test
    void atomicallyKeepsBackupAndRecoversMalformedPrimary() throws IOException {
        MapRegionCodec codec = new MapRegionCodec(() -> 1234L);
        Path primary = temporaryDirectory.resolve("r.0.0.hmap");
        int[] first = new int[MapRegion.PIXEL_COUNT];
        first[0] = 0xFF102030;
        int[] second = new int[MapRegion.PIXEL_COUNT];
        second[0] = 0xFF405060;
        codec.writeAtomic(primary, codec.encode(new MapRegion.Snapshot(0, 0, first, 1L)));
        codec.writeAtomic(primary, codec.encode(new MapRegion.Snapshot(0, 0, second, 2L)));
        Files.writeString(primary, "broken");

        MapRegion.Snapshot recovered = codec.readWithRecovery(primary, 0, 0);
        assertEquals(0xFF102030, recovered.pixel(0, 0));
        assertTrue(Files.exists(temporaryDirectory.resolve("r.0.0.corrupt-1234.hmap")));
        assertTrue(Files.exists(MapRegionCodec.backupPath(primary)));
        assertEquals(0xFF102030, codec.readWithRecovery(primary, 0, 0).pixel(0, 0));
    }

    @Test
    void preservesMalformedPrimaryWithoutBackupAndRecoversAsUndiscovered() throws IOException {
        MapRegionCodec codec = new MapRegionCodec(() -> 55L);
        Path primary = temporaryDirectory.resolve("r.-1.2.hmap");
        Files.writeString(primary, "broken");

        MapRegion.Snapshot recovered = codec.readWithRecovery(primary, -1, 2);

        assertEquals(0, recovered.pixel(0, 0));
        assertTrue(Files.exists(temporaryDirectory.resolve("r.-1.2.corrupt-55.hmap")));
    }

    private static byte[] gzip(byte[] raw) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(bytes)) {
            gzip.write(raw);
        }
        return bytes.toByteArray();
    }
}
