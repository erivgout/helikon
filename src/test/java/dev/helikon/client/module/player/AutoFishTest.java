package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoFishTest {
    @Test
    void castsOnceThenWaitsForADelayedBiteAndRecastWindow() {
        AutoFish module = enabled();

        assertEquals(AutoFish.Action.USE_HELD_ROD, module.update(0L, true, 100, false, false, false));
        assertEquals(AutoFish.Action.NONE, module.update(1L, true, 100, false, false, false));
        assertEquals(AutoFish.Action.NONE, module.update(2L, true, 100, true, false, true));
        assertEquals(AutoFish.Action.NONE, module.update(3L, true, 100, true, true, true));
        assertEquals(AutoFish.Action.USE_HELD_ROD, module.update(7L, true, 100, true, true, true));
        assertEquals(AutoFish.Action.NONE, module.update(8L, true, 100, true, true, true));
        assertEquals(AutoFish.Action.NONE, module.update(12L, true, 100, false, false, false));
        assertEquals(AutoFish.Action.USE_HELD_ROD, module.update(17L, true, 100, false, false, false));
    }

    @Test
    void stopsBeforeTheConfiguredDurabilityReserve() {
        assertEquals(AutoFish.Action.NONE, enabled().update(0L, true, 7, false, false, false));
    }

    private static AutoFish enabled() {
        AutoFish module = new AutoFish();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
