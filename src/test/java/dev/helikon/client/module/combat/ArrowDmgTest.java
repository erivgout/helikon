package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowDmgTest {
    @Test
    void defaultsOffAndDoesNothingUntilEnabled() {
        ArrowDmg arrowDmg = new ArrowDmg();

        assertEquals("arrow_dmg", arrowDmg.id());
        assertEquals(ModuleCategory.COMBAT, arrowDmg.category());
        assertFalse(arrowDmg.defaultEnabled());
        assertTrue(arrowDmg.planRelease(context(true, 20, 1.0D, 0.0D)).isEmpty());
    }

    @Test
    void fullyDrawnBowProducesBoundedMovementPlan() {
        ArrowDmg arrowDmg = enabled(new ArrowDmg());

        ArrowDmg.MovementPlan plan = arrowDmg.planRelease(context(true, 20, 1.0D, 0.0D)).orElseThrow();

        assertEquals(4, plan.stationaryPackets());
        assertEquals(-Math.sqrt(500.0D) * 0.3D, plan.offsetX(), 1.0E-9D);
        assertEquals(0.0D, plan.offsetZ(), 1.0E-9D);
        assertTrue(plan.sprintSignal());
        assertTrue(Math.hypot(plan.offsetX(), plan.offsetZ()) <= Math.sqrt(500.0D));
    }

    @Test
    void rejectsWrongItemEarlyReleaseAndVerticalAim() {
        ArrowDmg arrowDmg = enabled(new ArrowDmg());

        assertTrue(arrowDmg.planRelease(context(false, 20, 1.0D, 0.0D)).isEmpty());
        assertTrue(arrowDmg.planRelease(context(true, 19, 1.0D, 0.0D)).isEmpty());
        assertTrue(arrowDmg.planRelease(context(true, 20, 0.0D, 0.0D)).isEmpty());
        assertTrue(arrowDmg.planRelease(new ArrowDmg.ReleaseContext(true, 20, true,
                true, 1.0D, 0.0D)).isEmpty());
    }

    @Test
    void settingsEnforceConfiguredBounds() {
        ArrowDmg arrowDmg = new ArrowDmg();
        NumberSetting strength = (NumberSetting) setting(arrowDmg, "strength");
        IntegerSetting stationaryPackets = (IntegerSetting) setting(arrowDmg, "stationary_packets");

        assertThrows(IllegalArgumentException.class, () -> strength.set(10.1D));
        assertThrows(IllegalArgumentException.class, () -> stationaryPackets.set(0));
        assertThrows(IllegalArgumentException.class,
                () -> new ArrowDmg.ReleaseContext(true, -1, false, true, 1.0D, 0.0D));
    }

    private static ArrowDmg.ReleaseContext context(boolean bow, int drawTicks, double lookX, double lookZ) {
        return new ArrowDmg.ReleaseContext(bow, drawTicks, false, true, lookX, lookZ);
    }

    private static dev.helikon.client.setting.Setting<?> setting(ArrowDmg module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static ArrowDmg enabled(ArrowDmg module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
