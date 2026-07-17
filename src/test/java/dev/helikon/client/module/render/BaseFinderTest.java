package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseFinderTest {
    @Test
    void defaultsToBoundedClusterEvidenceAndStaysDisabled() {
        BaseFinder finder = new BaseFinder();
        BaseFinder.Evidence candidate = new BaseFinder.Evidence(0, 64, 0);

        assertEquals("base_finder", finder.id());
        assertEquals("BaseFinder", finder.name());
        assertEquals(ModuleCategory.RENDER, finder.category());
        assertFalse(finder.isEnabled());
        assertFalse(finder.defaultEnabled());
        assertTrue(finder.targetBlocks().contains("minecraft:crafting_table"));
        assertTrue(finder.scanBudget() >= 64 && finder.scanBudget() <= 2_048);
        assertFalse(finder.shouldHighlight(candidate, List.of(
                candidate,
                new BaseFinder.Evidence(2, 64, 0)
        )));
        assertTrue(finder.shouldHighlight(candidate, List.of(
                candidate,
                new BaseFinder.Evidence(2, 64, 0),
                new BaseFinder.Evidence(0, 67, 1)
        )));
        assertFalse(finder.shouldHighlight(candidate, List.of(
                candidate,
                new BaseFinder.Evidence(2, 64, 0),
                new BaseFinder.Evidence(20, 64, 0)
        )));
    }

    @Test
    void disableClearsTransientScannerState() {
        BaseFinder finder = new BaseFinder();
        boolean[] cleared = {false};
        finder.setCacheClearer(() -> cleared[0] = true);

        finder.enable();
        finder.disable();

        assertTrue(cleared[0]);
    }
}
