package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HighJumpTest {
    @Test
    void boostsOnlyTheFirstEligibleJumpTick() {
        HighJump highJump = enabled(new HighJump());

        assertEquals("high_jump", highJump.id());
        assertEquals(ModuleCategory.MOVEMENT, highJump.category());
        assertFalse(highJump.defaultEnabled());
        assertTrue(highJump.verticalVelocity(context(true, false, 0.0D)).isEmpty());
        assertEquals(0.70D, highJump.verticalVelocity(context(false, true, 0.42D)).orElseThrow());
        assertTrue(highJump.verticalVelocity(context(false, false, 0.30D)).isEmpty());
        assertTrue(highJump.verticalVelocity(context(false, false, 0.20D)).isEmpty());
    }

    @Test
    void preservesStrongerVanillaVelocityAndClearsStateForUnsafeContexts() {
        HighJump highJump = enabled(new HighJump());
        NumberSetting velocity = numberSetting(highJump, "jump_velocity");
        assertEquals(0.70D, velocity.value());
        assertEquals(0.42D, velocity.minimum());
        assertEquals(2.00D, velocity.maximum());

        assertTrue(highJump.verticalVelocity(context(true, false, 0.0D)).isEmpty());
        assertEquals(0.90D, highJump.verticalVelocity(context(false, true, 0.90D)).orElseThrow());
        assertTrue(highJump.verticalVelocity(context(false, true, 0.0D)).isEmpty());
        assertTrue(highJump.verticalVelocity(new HighJump.Context(false, false, true, false,
                true, false, false, false, 0.42D)).isEmpty());
        assertTrue(highJump.verticalVelocity(context(false, false, 0.42D)).isEmpty());
    }

    private static HighJump.Context context(boolean onGround, boolean jumpHeld, double velocity) {
        return new HighJump.Context(false, onGround, jumpHeld, false, false, false, false, false, velocity);
    }

    private static HighJump enabled(HighJump module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static NumberSetting numberSetting(HighJump module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
