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
    void saveAndLoadPreserveActiveModulesPresentation() {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        HudLayout source = new HudLayout();
        source.activeModules().setScale(1.5F);
        source.activeModules().setPadding(7);
        source.activeModules().setBackground(false);
        source.activeModules().setTextShadow(false);
        source.activeModules().setSort(dev.helikon.client.hud.ActiveModulesLayout.Sort.WIDTH);
        source.activeModules().setAlignment(dev.helikon.client.hud.ActiveModulesLayout.Alignment.RIGHT);
        source.activeModules().setColorMode(dev.helikon.client.hud.ActiveModulesLayout.ColorMode.RAINBOW);
        manager.save(source);

        HudLayout target = new HudLayout();
        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(target));
        assertEquals(1.5F, target.activeModules().scale());
        assertEquals(7, target.activeModules().padding());
        assertFalse(target.activeModules().background());
        assertFalse(target.activeModules().textShadow());
        assertEquals(dev.helikon.client.hud.ActiveModulesLayout.Sort.WIDTH, target.activeModules().sort());
        assertEquals(dev.helikon.client.hud.ActiveModulesLayout.Alignment.RIGHT, target.activeModules().alignment());
        assertEquals(dev.helikon.client.hud.ActiveModulesLayout.ColorMode.RAINBOW, target.activeModules().colorMode());
    }

    @Test
    void saveAndLoadPreserveHudElementPlacements() {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        HudLayout source = new HudLayout();
        source.element(dev.helikon.client.hud.HudElementId.RADAR).setEnabled(false);
        source.element(dev.helikon.client.hud.HudElementId.RADAR)
                .set(dev.helikon.client.hud.HudElementId.Anchor.TOP_RIGHT, 9, 12);
        manager.save(source);

        HudLayout target = new HudLayout();
        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(target));
        var radar = target.element(dev.helikon.client.hud.HudElementId.RADAR);
        assertFalse(radar.enabled());
        assertEquals(dev.helikon.client.hud.HudElementId.Anchor.TOP_RIGHT, radar.anchor());
        assertEquals(9, radar.offsetX());
        assertEquals(12, radar.offsetY());
    }

    @Test
    void saveAndLoadPreserveHudElementPresentation() {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        HudLayout source = new HudLayout();
        var placement = source.element(dev.helikon.client.hud.HudElementId.SATURATION);
        placement.setScale(1.5F);
        placement.setAlignment(dev.helikon.client.hud.HudElementPlacement.Alignment.CENTER);
        placement.setBackground(false);
        placement.setPadding(7);
        placement.setTextShadow(false);
        placement.setColor(0xFF80D8FF);
        placement.setRainbow(true);
        manager.save(source);

        HudLayout target = new HudLayout();
        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(target));
        var restored = target.element(dev.helikon.client.hud.HudElementId.SATURATION);
        assertEquals(1.5F, restored.scale());
        assertEquals(dev.helikon.client.hud.HudElementPlacement.Alignment.CENTER, restored.alignment());
        assertFalse(restored.background());
        assertEquals(7, restored.padding());
        assertFalse(restored.textShadow());
        assertEquals(0xFF80D8FF, restored.color());
        assertTrue(restored.rainbow());
    }

    @Test
    void seedCrackerHudPlacementIsPersistedLikeOtherModuleHudElements() {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        HudLayout source = new HudLayout();
        var placement = source.element(dev.helikon.client.hud.HudElementId.SEED_CRACKER);
        placement.set(dev.helikon.client.hud.HudElementId.Anchor.BOTTOM_RIGHT, 17, 23);
        placement.setScale(0.75F);
        manager.save(source);

        HudLayout target = new HudLayout();
        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(target));
        var restored = target.element(dev.helikon.client.hud.HudElementId.SEED_CRACKER);
        assertEquals(dev.helikon.client.hud.HudElementId.Anchor.BOTTOM_RIGHT, restored.anchor());
        assertEquals(17, restored.offsetX());
        assertEquals(23, restored.offsetY());
        assertEquals(0.75F, restored.scale());
    }

    @Test
    void malformedConfigurationIsPreservedAndRestoresDefaults() throws IOException {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.hudConfigurationPath().getParent());
        Files.writeString(manager.hudConfigurationPath(), "not valid json");

        HudLayout layout = new HudLayout();
        layout.setActiveModulesEnabled(false);
        layout.setActiveModulesPosition(50, 50);
        layout.element(dev.helikon.client.hud.HudElementId.SATURATION).setEnabled(false);
        layout.element(dev.helikon.client.hud.HudElementId.SATURATION)
                .setAbsolutePosition(500, 500);

        assertEquals(HudConfigurationManager.LoadResult.RECOVERED_FROM_ERROR, manager.load(layout));
        assertTrue(layout.activeModulesEnabled());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_X, layout.activeModulesX());
        assertEquals(HudLayout.DEFAULT_ACTIVE_MODULES_Y, layout.activeModulesY());
        assertTrue(layout.element(dev.helikon.client.hud.HudElementId.SATURATION).enabled());
        assertEquals(dev.helikon.client.hud.HudElementId.SATURATION.defaultAnchor(),
                layout.element(dev.helikon.client.hud.HudElementId.SATURATION).anchor());
        try (var files = Files.list(manager.hudConfigurationPath().getParent())) {
            assertTrue(files.anyMatch(path -> path.getFileName().toString().startsWith("hud.corrupt-")));
        }
    }

    @Test
    void missingActiveModulesResetsTelemetryPlacementsToo() throws IOException {
        HudConfigurationManager manager = new HudConfigurationManager(temporaryDirectory.resolve("helikon"));
        Files.createDirectories(manager.hudConfigurationPath().getParent());
        Files.writeString(manager.hudConfigurationPath(), "{\"schemaVersion\":3}");
        HudLayout layout = new HudLayout();
        layout.element(dev.helikon.client.hud.HudElementId.REACH).setEnabled(false);
        layout.element(dev.helikon.client.hud.HudElementId.REACH).setAbsolutePosition(400, 400);

        assertEquals(HudConfigurationManager.LoadResult.LOADED, manager.load(layout));
        assertTrue(layout.element(dev.helikon.client.hud.HudElementId.REACH).enabled());
        assertEquals(dev.helikon.client.hud.HudElementId.REACH.defaultAnchor(),
                layout.element(dev.helikon.client.hud.HudElementId.REACH).anchor());
    }
}
