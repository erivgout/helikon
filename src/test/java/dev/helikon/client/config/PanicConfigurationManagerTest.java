package dev.helikon.client.config;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.input.PanicKeybindManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PanicConfigurationManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void persistsKeybindAndRecoversInvalidConfiguration() throws IOException {
        PanicConfigurationManager configuration = new PanicConfigurationManager(temporaryDirectory.resolve("helikon"));
        PanicKeybindManager keybinds = new PanicKeybindManager();
        configuration.setKeybindAndSave(keybinds, new Keybind(82, Keybind.Activation.TOGGLE));
        configuration.setKeybindAndSave(keybinds, new Keybind(83, Keybind.Activation.TOGGLE));

        PanicKeybindManager loaded = new PanicKeybindManager();
        assertEquals(PanicConfigurationManager.LoadResult.LOADED, configuration.load(loaded));
        assertEquals(83, loaded.keybind().keyCode());
        assertTrue(Files.exists(temporaryDirectory.resolve("helikon").resolve("panic.json.bak")));

        Files.writeString(temporaryDirectory.resolve("helikon").resolve("panic.json"), "{\"schemaVersion\":1,\"key\":99999}");
        assertEquals(PanicConfigurationManager.LoadResult.RECOVERED_FROM_ERROR, configuration.load(loaded));
        assertFalse(loaded.keybind().isBound());
    }

    @Test
    void rollsBackKeybindWhenAtomicSaveCannotStart() throws IOException {
        Path nonDirectory = temporaryDirectory.resolve("not-a-directory");
        Files.writeString(nonDirectory, "not a directory");
        PanicConfigurationManager configuration = new PanicConfigurationManager(nonDirectory);
        PanicKeybindManager keybinds = new PanicKeybindManager();

        assertThrows(ConfigurationException.class,
                () -> configuration.setKeybindAndSave(keybinds, new Keybind(82, Keybind.Activation.TOGGLE)));

        assertFalse(keybinds.keybind().isBound());
    }

    @Test
    void treatsPersistedGuiKeyAsInvalidAndRecoversSafely() throws IOException {
        Path directory = temporaryDirectory.resolve("helikon");
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("panic.json"), "{\"schemaVersion\":1,\"key\":344}");
        PanicConfigurationManager configuration = new PanicConfigurationManager(directory, key -> key == 344);
        PanicKeybindManager keybinds = new PanicKeybindManager();

        assertEquals(PanicConfigurationManager.LoadResult.RECOVERED_FROM_ERROR, configuration.load(keybinds));
        assertFalse(keybinds.keybind().isBound());
        try (var files = Files.list(directory)) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("panic.corrupt-")));
        }
    }
}
