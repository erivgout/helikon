package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapStorageKeyTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void hashesScopeAndDimensionIntoSafeDeterministicPaths() {
        WaypointContext overworld = new WaypointContext("server:example.org:25565", "minecraft:overworld");
        MapStorageKey first = new MapStorageKey(temporaryDirectory, overworld);
        MapStorageKey second = new MapStorageKey(temporaryDirectory, overworld);
        MapStorageKey nether = new MapStorageKey(temporaryDirectory,
                new WaypointContext(overworld.scope(), "minecraft:the_nether"));

        assertEquals(first.scopeToken(), second.scopeToken());
        assertEquals(first.dimensionToken(), second.dimensionToken());
        assertEquals(64, first.scopeToken().length());
        assertTrue(first.scopeToken().matches("[0-9a-f]{64}"));
        assertNotEquals(first.dimensionToken(), nether.dimensionToken());
        assertTrue(first.directory().startsWith(temporaryDirectory.toAbsolutePath()));
        assertTrue(first.regionPath(-2, 3).getFileName().toString().equals("r.-2.3.hmap"));
    }

    @Test
    void validatesSelfIdentifyingMetadataWithoutReplacingMismatch() throws IOException {
        WaypointContext context = new WaypointContext("world:alpha", "minecraft:overworld");
        MapStorageKey key = new MapStorageKey(temporaryDirectory, context);
        key.ensureMetadata();
        String original = Files.readString(key.metadataPath());
        key.ensureMetadata();
        assertEquals(original, Files.readString(key.metadataPath()));

        Files.writeString(key.metadataPath(), "{\"schemaVersion\":1,\"scope\":\"world:other\","
                + "\"dimension\":\"minecraft:overworld\"}");
        assertThrows(MapStorageKey.MapMetadataException.class, key::ensureMetadata);
        assertTrue(Files.readString(key.metadataPath()).contains("world:other"));
    }

    @Test
    void leavesNewerMetadataUntouched() throws IOException {
        MapStorageKey key = new MapStorageKey(temporaryDirectory,
                new WaypointContext("world:newer", "minecraft:overworld"));
        Files.createDirectories(key.directory());
        Files.writeString(key.metadataPath(), "{\"schemaVersion\":2,\"scope\":\"world:newer\","
                + "\"dimension\":\"minecraft:overworld\"}");

        assertThrows(MapStorageKey.UnsupportedMetadataVersionException.class, key::ensureMetadata);
        assertTrue(Files.readString(key.metadataPath()).contains("\"schemaVersion\":2"));
    }
}

