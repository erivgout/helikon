package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FishTest {
    @Test
    void appliesBoundedInputVelocityOnlyForEligibleUnderwaterSwimming() {
        Fish module = new Fish();
        Fish.Context forwardAndUp = context(true, false, false, false, false, true, true, false);
        assertFalse(module.velocity(forwardAndUp).isPresent());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        Fish.Velocity velocity = module.velocity(forwardAndUp).orElseThrow();
        assertEquals(0.15D, velocity.horizontal().x(), 0.0001D);
        assertEquals(0.0D, velocity.horizontal().z(), 0.0001D);
        assertEquals(0.12D, velocity.vertical(), 0.0001D);

        numberSetting(module, "horizontal_speed").set(0.30D);
        numberSetting(module, "vertical_speed").set(0.20D);
        Fish.Velocity leftAndDown = module.velocity(context(true, false, false, false, false, true, false, true))
                .orElseThrow();
        assertEquals(0.0D, leftAndDown.horizontal().x(), 0.0001D);
        assertEquals(0.30D, leftAndDown.horizontal().z(), 0.0001D);
        assertEquals(-0.20D, leftAndDown.vertical(), 0.0001D);

        assertTrue(module.velocity(context(true, true, false, false, false, true, false, false)).isEmpty());
        assertTrue(module.velocity(context(false, false, false, false, false, true, false, false)).isEmpty());
        assertTrue(module.velocity(context(true, false, true, false, false, true, false, false)).isEmpty());
        assertTrue(module.velocity(context(true, false, false, true, false, true, false, false)).isEmpty());
        assertTrue(module.velocity(context(true, false, false, false, true, true, false, false)).isEmpty());
    }

    @Test
    void leavesUncontrolledVelocityUntouched() {
        Fish module = new Fish();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        Fish.Context idle = new Fish.Context(true, false, false, false, false, false, false, false,
                new HorizontalVelocity(0.0D, 0.0D), new HorizontalVelocity(0.08D, -0.03D), -0.04D);
        assertTrue(module.velocity(idle).isEmpty());
    }

    private static Fish.Context context(boolean inWater, boolean screenOpen, boolean passenger, boolean abilityFlying,
                                        boolean fallFlying, boolean moving, boolean jumping, boolean sneaking) {
        HorizontalVelocity direction = moving ? new HorizontalVelocity(jumping ? 1.0D : 0.0D, jumping ? 0.0D : 1.0D)
                : new HorizontalVelocity(0.0D, 0.0D);
        return new Fish.Context(inWater, screenOpen, passenger, abilityFlying, fallFlying, moving, jumping, sneaking,
                direction, new HorizontalVelocity(0.02D, 0.01D), -0.03D);
    }

    private static NumberSetting numberSetting(Fish module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
