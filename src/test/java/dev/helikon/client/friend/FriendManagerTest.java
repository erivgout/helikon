package dev.helikon.client.friend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void addsTogglesColorsAndRoundTripsLocally() {
        FriendManager source = new FriendManager(temporaryDirectory.resolve("helikon"));
        assertTrue(source.add("Alice_1"));
        assertFalse(source.add("alice_1"));
        assertTrue(source.setColor("ALICE_1", 0x80402010));
        assertFalse(source.toggle("alice_1"));
        assertTrue(source.toggle("Alice_1"));
        source.save();
        source.setColor("alice_1", 0xFF123456);
        source.save();

        FriendManager loaded = new FriendManager(temporaryDirectory.resolve("helikon"));
        assertEquals(FriendManager.LoadResult.LOADED, loaded.load());
        assertEquals(0xFF123456, loaded.find("alice_1").orElseThrow().color());
        assertTrue(Files.exists(temporaryDirectory.resolve("helikon").resolve("friends.json.bak")));
    }

    @Test
    void rejectsUnsafeNamesAndRecoversMalformedFiles() throws IOException {
        FriendManager manager = new FriendManager(temporaryDirectory.resolve("helikon"));
        assertThrows(IllegalArgumentException.class, () -> manager.add("bad name"));
        assertFalse(manager.contains("§r§7§4§4§7§m§a§f"));
        assertTrue(manager.find(".SweatyLeaf02241").isEmpty());
        Path directory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("friends.json"), "{\"schemaVersion\":1,\"friends\":[{\"name\":\"bad name\",\"color\":0}]}" );

        assertEquals(FriendManager.LoadResult.RECOVERED_FROM_ERROR, manager.load());
        assertTrue(manager.list().isEmpty());
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("friends.corrupt-")));
        }
    }

    @Test
    void recoversCaseInsensitiveDuplicatePersistedNames() throws IOException {
        Path directory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("friends.json"), """
                {"schemaVersion":1,"friends":[
                  {"name":"Alice_1","color":0},
                  {"name":"alice_1","color":1}
                ]}
                """);

        FriendManager manager = new FriendManager(directory);

        assertEquals(FriendManager.LoadResult.RECOVERED_FROM_ERROR, manager.load());
        assertTrue(manager.list().isEmpty());
    }
}
