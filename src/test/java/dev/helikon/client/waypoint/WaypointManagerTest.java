package dev.helikon.client.waypoint;

import dev.helikon.client.config.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaypointManagerTest {
    private static final WaypointContext OVERWORLD = new WaypointContext("world:tests", "minecraft:overworld");
    private static final WaypointContext NETHER = new WaypointContext("world:tests", "minecraft:the_nether");

    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsContextualWaypointsAndKeepsBackup() {
        WaypointManager source = new WaypointManager(temporaryDirectory.resolve("helikon"), () -> 1234L);
        assertTrue(source.add("Home", 10, 64, -20, OVERWORLD));
        assertTrue(source.add("Home", 1, 70, 2, NETHER));
        assertFalse(source.add("home", 0, 64, 0, OVERWORLD));
        assertTrue(source.setColor("HOME", OVERWORLD, 0x80402010));
        assertTrue(source.setIcon("home", OVERWORLD, "house"));
        assertEquals(false, source.toggle("home", OVERWORLD).orElseThrow());
        source.save();

        assertTrue(source.setColor("home", OVERWORLD, 0xFF123456));
        source.save();

        WaypointManager loaded = new WaypointManager(temporaryDirectory.resolve("helikon"));
        assertEquals(WaypointManager.LoadResult.LOADED, loaded.load());
        Waypoint home = loaded.find("home", OVERWORLD).orElseThrow();
        assertEquals(0xFF123456, home.color());
        assertEquals("house", home.icon());
        assertFalse(home.enabled());
        assertEquals(1234L, home.createdAtEpochMillis());
        assertTrue(loaded.find("home", NETHER).isPresent());
        assertTrue(Files.exists(temporaryDirectory.resolve("helikon").resolve("waypoints.json.bak")));
    }

    @Test
    void validatesEntriesAndRecoversMalformedDuplicateData() throws IOException {
        WaypointManager manager = new WaypointManager(temporaryDirectory.resolve("helikon"));
        assertThrows(IllegalArgumentException.class, () -> manager.add("bad/name", 0, 64, 0, OVERWORLD));

        Path directory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("waypoints.json"), """
                {"schemaVersion":1,"waypoints":[
                  {"name":"Home","x":0,"y":64,"z":0,"scope":"world:tests","dimension":"minecraft:overworld","color":0,"enabled":true,"createdAt":1},
                  {"name":"home","x":1,"y":64,"z":1,"scope":"world:tests","dimension":"minecraft:overworld","color":0,"enabled":true,"createdAt":1}
                ]}
                """);

        assertEquals(WaypointManager.LoadResult.RECOVERED_FROM_ERROR, manager.load());
        assertTrue(manager.list().isEmpty());
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("waypoints.corrupt-")));
        }
    }

    @Test
    void renameAndRemovalAreScopedToTheCurrentDimension() {
        WaypointManager manager = new WaypointManager(temporaryDirectory.resolve("helikon"), () -> 1L);
        manager.add("Home", 0, 64, 0, OVERWORLD);
        manager.add("Home", 0, 64, 0, NETHER);

        assertTrue(manager.rename("home", "Base", OVERWORLD));
        assertTrue(manager.find("base", OVERWORLD).isPresent());
        assertTrue(manager.find("home", NETHER).isPresent());
        assertTrue(manager.remove("base", OVERWORLD));
        assertTrue(manager.find("home", NETHER).isPresent());
    }

    @Test
    void commandStyleMutationRollsBackWhenItsSaveFails() throws IOException {
        Path nonDirectory = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(nonDirectory, "not a directory");
        WaypointManager manager = new WaypointManager(nonDirectory, () -> 1L);

        assertThrows(ConfigurationException.class, () -> manager.addAndSave("Home", 0, 64, 0, OVERWORLD));

        assertTrue(manager.list().isEmpty());
        assertFalse(manager.find("home", OVERWORLD).isPresent());
    }
}
