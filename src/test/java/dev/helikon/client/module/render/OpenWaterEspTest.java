package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenWaterEspTest {
    @Test
    void hasSafeIdentityDefaultsAndBoundedScanSettings() {
        OpenWaterEsp module = new OpenWaterEsp();

        assertEquals("open_water_esp", module.id());
        assertEquals("OpenWaterESP", module.name());
        assertEquals(ModuleCategory.RENDER, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(16, module.horizontalRange());
        assertEquals(12, module.verticalSearch());
        assertEquals(8, module.scanBudget());
        assertEquals(128, module.maximumSites());

        IntegerSetting range = integerSetting(module, "horizontal_range");
        IntegerSetting budget = integerSetting(module, "scan_budget");
        assertEquals(8, range.minimum());
        assertEquals(32, range.maximum());
        assertEquals(1, budget.minimum());
        assertEquals(32, budget.maximum());
        assertThrows(IllegalArgumentException.class, () -> budget.set(33));
    }

    @Test
    void acceptsUniformWaterThenAboveWaterLayers() {
        OpenWaterEsp.CellType[] cells = layers(
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER);

        assertTrue(OpenWaterEsp.isValidSite(cells));
    }

    @Test
    void rejectsMixedObstructedAndReversedLayers() {
        OpenWaterEsp.CellType[] mixed = layers(
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER);
        mixed[OpenWaterEsp.CELLS_PER_LAYER + 7] = OpenWaterEsp.CellType.INVALID;
        assertFalse(OpenWaterEsp.isValidSite(mixed));

        assertFalse(OpenWaterEsp.isValidSite(layers(
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.INSIDE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER)));
        assertFalse(OpenWaterEsp.isValidSite(layers(
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER,
                OpenWaterEsp.CellType.ABOVE_WATER)));
        assertFalse(OpenWaterEsp.isValidSite(new OpenWaterEsp.CellType[1]));
    }

    @Test
    void disableClearsAdapterState() {
        OpenWaterEsp module = new OpenWaterEsp();
        AtomicBoolean cleared = new AtomicBoolean();
        module.setCacheClearer(() -> cleared.set(true));

        module.enable();
        module.disable();

        assertTrue(cleared.get());
    }

    private static OpenWaterEsp.CellType[] layers(OpenWaterEsp.CellType... types) {
        OpenWaterEsp.CellType[] cells =
                new OpenWaterEsp.CellType[OpenWaterEsp.LAYER_COUNT * OpenWaterEsp.CELLS_PER_LAYER];
        for (int layer = 0; layer < types.length; layer++) {
            Arrays.fill(cells, layer * OpenWaterEsp.CELLS_PER_LAYER,
                    (layer + 1) * OpenWaterEsp.CELLS_PER_LAYER, types[layer]);
        }
        return cells;
    }

    private static IntegerSetting integerSetting(OpenWaterEsp module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
