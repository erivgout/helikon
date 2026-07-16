package dev.helikon.client.config;

import dev.helikon.client.hud.HudLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudConfigurationManagerTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void saveAndLoadPreserveActiveModulesLayout() {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        HudLayout source = new HudLayout();
        source.setActiveModulesEnabled(false);
        source.setActiveModulesPosition(64, 96);
        manager.save(source);

        HudLayout target = new HudLayout();
        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(target));
        assertFalse(target.activeModulesEnabled());
        assertEquals(64, target.activeModulesX());
        assertEquals(96, target.activeModulesY());
    }

    @Test
    void invalidPropertiesResetToSafeDefaultsWithoutDiscardingTheWholeFile() throws IOException {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.hudConfigurationPath().getParent());
        Files.writeString(manager.hudConfigurationPath(), """
                {
                  "schemaVersion": 1,
                  "activeModules": {
                    "enabled": "yes",
                    "x": -1,
                    "y": 10001
                  }
                }
                """);

        HudLayout layout = new HudLayout();
        layout.setActiveModulesEnabled(false);
        layout.setActiveModulesPosition(50, 50);

        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(layout));
        assertTrue(layout.activeModulesEnabled());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_X, layout.activeModulesX());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_Y, layout.activeModulesY());
    }

    @Test
    void malformedConfigurationIsPreservedAndRestoresDefaults() throws IOException {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.hudConfigurationPath().getParent());
        Files.writeString(manager.hudConfigurationPath(), "not valid json");

        HudLayout layout = new HudLayout();
        layout.setActiveModulesEnabled(false);
        layout.setActiveModulesPosition(50, 50);

        assertEquals(HudConfigurationManager.LoadResult.RECOVERED_FROM_ERROR, manager.load(layout));
        assertTrue(layout.activeModulesEnabled());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_X, layout.activeModulesX());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_Y, layout.activeModulesY());
        try (var files = Files.list(manager.hudConfigurationPath().getParent())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("hud.corrupt-")));
        }
    }
}
