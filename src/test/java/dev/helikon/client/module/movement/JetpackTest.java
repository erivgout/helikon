package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.OptionalDouble;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JetpackTest {
    @Test
    void identityAndDefaults() {
        Jetpack jetpack = new Jetpack();
        assertEquals("jetpack", jetpack.id());
        assertEquals(ModuleCategory.MOVEMENT, jetpack.category());
        assertFalse(jetpack.defaultEnabled(), "advantage movement modules default off");
    }

    @Test
    void ascendsOnlyWhileEnabledAndJumpHeld() {
        Jetpack jetpack = new Jetpack();

        // Disabled: never imposes velocity, even with jump held.
        assertTrue(jetpack.ascentVelocity(true).isEmpty());

        enable(jetpack);

        // Enabled but jump released: leaves vanilla motion (normal fall).
        assertTrue(jetpack.ascentVelocity(false).isEmpty());

        // Enabled and jump held: imposes the configured upward velocity.
        OptionalDouble rising = jetpack.ascentVelocity(true);
        assertTrue(rising.isPresent());
        assertEquals(0.42D, rising.getAsDouble(), 1.0E-9D);
    }

    @Test
    void ascentIsClampedToMaxRiseSpeed() {
        Jetpack jetpack = new Jetpack();
        enable(jetpack);
        number(jetpack, "ascend_speed").set(1.5D);
        number(jetpack, "max_rise_speed").set(0.5D);

        assertEquals(0.5D, jetpack.ascentVelocity(true).getAsDouble(), 1.0E-9D);
    }

    @Test
    void disablingStopsAscent() {
        Jetpack jetpack = new Jetpack();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(jetpack);
        registry.setEnabled(jetpack, true);
        assertTrue(jetpack.ascentVelocity(true).isPresent());

        registry.setEnabled(jetpack, false);
        assertTrue(jetpack.ascentVelocity(true).isEmpty());
    }

    private static void enable(Jetpack jetpack) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(jetpack);
        registry.setEnabled(jetpack, true);
    }

    private static NumberSetting number(Jetpack module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
