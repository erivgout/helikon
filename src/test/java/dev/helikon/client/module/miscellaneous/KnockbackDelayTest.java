package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnockbackDelayTest {
    private static final KnockbackDelay.Motion FIRST = new KnockbackDelay.Motion(0.5D, 0.4D, -0.25D);
    private static final KnockbackDelay.Motion SECOND = new KnockbackDelay.Motion(-0.2D, 0.1D, 0.75D);

    @Test
    void defaultsOffWithTheUtilityCategoryAndPassesMotionThrough() {
        KnockbackDelay module = new KnockbackDelay(motion -> {
        });

        assertEquals("knockback_delay", module.id());
        assertEquals(ModuleCategory.MISCELLANEOUS, module.category());
        assertFalse(module.defaultEnabled());
        assertFalse(module.delay(0L, FIRST));
        assertEquals(0, module.pendingCount());
    }

    @Test
    void releasesFiniteMotionAfterTheConfiguredDelayInArrivalOrder() {
        KnockbackDelay module = new KnockbackDelay(motion -> {
        });
        module.enable();

        assertTrue(module.delay(10L, FIRST));
        assertTrue(module.delay(11L, SECOND));
        assertEquals(List.of(), module.releaseReady(14L));
        assertEquals(List.of(FIRST), module.releaseReady(15L));
        assertEquals(List.of(SECOND), module.releaseReady(16L));
        assertEquals(0, module.pendingCount());
    }

    @Test
    void disableImmediatelyFlushesPendingVanillaMotion() {
        List<KnockbackDelay.Motion> applied = new ArrayList<>();
        KnockbackDelay module = new KnockbackDelay(applied::add);
        module.enable();
        assertTrue(module.delay(2L, FIRST));
        assertTrue(module.delay(3L, SECOND));

        module.disable();

        assertEquals(List.of(FIRST, SECOND), applied);
        assertEquals(0, module.pendingCount());
    }

    @Test
    void rejectsUnsafeInputsAndBoundsTheDelaySetting() {
        KnockbackDelay module = new KnockbackDelay(motion -> {
        });
        IntegerSetting delay = (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("delay_ticks"))
                .findFirst()
                .orElseThrow();

        assertEquals(1, delay.minimum());
        assertEquals(40, delay.maximum());
        assertThrows(IllegalArgumentException.class, () -> delay.set(0));
        assertThrows(IllegalArgumentException.class, () -> delay.set(41));
        assertThrows(IllegalArgumentException.class, () -> module.delay(-1L, FIRST));

        module.enable();
        assertFalse(module.delay(0L, new KnockbackDelay.Motion(Double.NaN, 0.0D, 0.0D)));
    }
}
