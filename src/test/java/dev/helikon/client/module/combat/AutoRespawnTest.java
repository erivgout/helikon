package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoRespawnTest {
    @Test
    void requestsExactlyOnceForEachObservedDeathAndResetsAfterRespawn() {
        AutoRespawn autoRespawn = enabled(new AutoRespawn());

        assertEquals("auto_respawn", autoRespawn.id());
        assertEquals(ModuleCategory.COMBAT, autoRespawn.category());
        assertFalse(autoRespawn.defaultEnabled());
        assertFalse(autoRespawn.shouldRequestRespawn(0L, false));
        assertTrue(autoRespawn.shouldRequestRespawn(1L, true));
        assertFalse(autoRespawn.shouldRequestRespawn(2L, true));
        assertFalse(autoRespawn.shouldRequestRespawn(3L, false));
        assertTrue(autoRespawn.shouldRequestRespawn(4L, true));
    }

    @Test
    void delayIsBoundedAndDisableClearsAQueuedDeath() {
        AutoRespawn autoRespawn = enabled(new AutoRespawn());
        IntegerSetting delay = integerSetting(autoRespawn, "delay_ticks");
        assertEquals(0, delay.value());
        assertEquals(0, delay.minimum());
        assertEquals(100, delay.maximum());
        delay.set(3);

        assertFalse(autoRespawn.shouldRequestRespawn(10L, true));
        assertFalse(autoRespawn.shouldRequestRespawn(12L, true));
        autoRespawn.disable();
        assertFalse(autoRespawn.shouldRequestRespawn(13L, true));
        autoRespawn.enable();
        assertFalse(autoRespawn.shouldRequestRespawn(13L, true));
        assertTrue(autoRespawn.shouldRequestRespawn(16L, true));
    }

    private static AutoRespawn enabled(AutoRespawn module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static IntegerSetting integerSetting(AutoRespawn module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
