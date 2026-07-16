package dev.helikon.client.macro;

import dev.helikon.client.config.ConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MacroManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsExplicitActionsAndServerScopeLocally() {
        MacroManager source = new MacroManager(temporaryDirectory.resolve("helikon"));
        assertTrue(source.createAndSave("greeting", Macro.GLOBAL));
        assertTrue(source.addActionAndSave("greeting", MacroAction.chat("Hello world")));
        assertTrue(source.addActionAndSave("greeting", MacroAction.delay(4)));
        assertTrue(source.setServerAddressAndSave("greeting", "Example.Org:25565"));

        MacroManager loaded = new MacroManager(temporaryDirectory.resolve("helikon"));
        assertEquals(MacroManager.LoadResult.LOADED, loaded.load());
        Macro macro = loaded.find("GREETING").orElseThrow();
        assertEquals("example.org:25565", macro.serverAddress());
        assertEquals(List.of(MacroAction.chat("Hello world"), MacroAction.delay(4)), macro.actions());
        assertTrue(Files.exists(temporaryDirectory.resolve("helikon").resolve("macros.json.bak")));
    }

    @Test
    void rejectsUnsafeActionsAndRecoversDuplicatePersistedMacros() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> MacroAction.chat(".would-leak"));
        assertThrows(IllegalArgumentException.class, () -> MacroAction.command("/say no"));
        assertThrows(IllegalArgumentException.class, () -> MacroAction.local(".macro run loop"));

        Path directory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("macros.json"), """
                {"schemaVersion":1,"macros":[
                  {"name":"test","actions":[]},
                  {"name":"TEST","actions":[]}
                ]}
                """);
        MacroManager manager = new MacroManager(directory);

        assertEquals(MacroManager.LoadResult.RECOVERED_FROM_ERROR, manager.load());
        assertTrue(manager.list().isEmpty());
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("macros.corrupt-")));
        }
    }

    @Test
    void transactionalMutationRollsBackIfSavingFails() throws IOException {
        Path nonDirectory = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(nonDirectory, "not a directory");
        MacroManager manager = new MacroManager(nonDirectory);

        assertThrows(ConfigurationException.class, () -> manager.createAndSave("test", Macro.GLOBAL));

        assertFalse(manager.find("test").isPresent());
    }
}
