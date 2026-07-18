package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirJumpTest {
    @Test
    void appliesOneAirJumpForEachFreshPress() {
        AirJump airJump = enabled(new AirJump());

        assertEquals("air_jump", airJump.id());
        assertEquals(ModuleCategory.MOVEMENT, airJump.category());
        assertFalse(airJump.defaultEnabled());
        assertEquals(0.42D, airJump.verticalVelocity(context(false, true, -0.2D)).orElseThrow());
        for (int tick = 1; tick < 6; tick++) {
            assertTrue(airJump.verticalVelocity(context(false, true, -0.1D)).isEmpty());
        }
        assertEquals(0.42D, airJump.verticalVelocity(context(false, true, -0.1D)).orElseThrow());
        assertTrue(airJump.verticalVelocity(context(false, false, -0.2D)).isEmpty());
        assertEquals(0.42D, airJump.verticalVelocity(context(false, true, 0.1D)).orElseThrow());
    }

    @Test
    void preservesStrongerRiseAndRejectsUnsafeContexts() {
        AirJump airJump = enabled(new AirJump());
        NumberSetting velocity = (NumberSetting) airJump.settings().stream()
                .filter(setting -> setting.id().equals("jump_velocity"))
                .findFirst()
                .orElseThrow();

        assertEquals(0.10D, velocity.minimum());
        assertEquals(1.50D, velocity.maximum());
        assertTrue(airJump.settings().stream()
                .filter(setting -> setting.id().equals("repeat_while_held"))
                .findFirst().orElseThrow().value() instanceof Boolean enabled && enabled);
        assertEquals(0.8D, airJump.verticalVelocity(context(false, true, 0.8D)).orElseThrow());
        airJump.verticalVelocity(context(false, false, 0.0D));
        assertTrue(airJump.verticalVelocity(context(true, true, 0.0D)).isEmpty());
        airJump.verticalVelocity(context(false, false, 0.0D));
        assertTrue(airJump.verticalVelocity(new AirJump.Context(false, false, true, true,
                false, false, false, false, 0.0D)).isEmpty());
    }

    private static AirJump.Context context(boolean onGround, boolean jumpHeld, double velocity) {
        return new AirJump.Context(false, onGround, jumpHeld, false,
                false, false, false, false, velocity);
    }

    private static AirJump enabled(AirJump module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
