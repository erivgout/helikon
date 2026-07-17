package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CaveFinderTest {
    @Test
    void defaultsRecognizeOnlySampledWalkableBuriedSpace() {
        CaveFinder finder = new CaveFinder();
        CaveFinder.CaveSample cave = sample(true, true, true, 2, true);

        assertEquals("cave_finder", finder.id());
        assertEquals("CaveFinder", finder.name());
        assertEquals(ModuleCategory.RENDER, finder.category());
        assertFalse(finder.isEnabled());
        assertFalse(finder.defaultEnabled());
        assertTrue(finder.shouldMark(cave));
        assertFalse(finder.shouldMark(sample(true, false, true, 2, true)));
        assertFalse(finder.shouldMark(sample(true, true, true, 1, true)));
        assertFalse(finder.shouldMark(sample(true, true, true, 2, false)));
        assertFalse(finder.shouldMark(new CaveFinder.CaveSample(
                1, 52, 0, 64, true, true, true, 2, true)));
        assertTrue(finder.withinCurrentRange(cave, 0.0D, 64.0D, 0.0D));
        assertFalse(finder.withinCurrentRange(cave, 100.0D, 64.0D, 0.0D));
    }

    @Test
    void scanBudgetRejectsValuesOutsideItsDocumentedBounds() {
        CaveFinder finder = new CaveFinder();
        IntegerSetting budget = (IntegerSetting) finder.settings().stream()
                .filter(setting -> setting.id().equals("scan_budget"))
                .findFirst()
                .orElseThrow();

        assertEquals(64, budget.minimum());
        assertEquals(2_048, budget.maximum());
        assertThrows(IllegalArgumentException.class, () -> budget.set(63));
        assertThrows(IllegalArgumentException.class, () -> budget.set(2_049));
    }

    @Test
    void disableClearsTransientScannerState() {
        CaveFinder finder = new CaveFinder();
        boolean[] cleared = {false};
        finder.setCacheClearer(() -> cleared[0] = true);

        finder.enable();
        finder.disable();

        assertTrue(cleared[0]);
    }

    private static CaveFinder.CaveSample sample(
            boolean feetAir,
            boolean headAir,
            boolean floorCollision,
            int openSides,
            boolean belowSurface
    ) {
        return new CaveFinder.CaveSample(
                0, 52, 0, 64, feetAir, headAir, floorCollision, openSides, belowSurface);
    }
}
