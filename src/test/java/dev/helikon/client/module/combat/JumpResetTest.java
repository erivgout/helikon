package dev.helikon.client.module.combat;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JumpResetTest {
    private static final JumpResetContext FRESH_MOVING_HIT = new JumpResetContext(false, true, true, true);

    @Test
    void isDisabledByDefaultAndRequiresAGroundedFreshHit() {
        JumpReset module = new JumpReset();
        assertFalse(module.shouldJumpReset(0L, FRESH_MOVING_HIT));

        JumpReset enabled = enabled();
        assertTrue(enabled.shouldJumpReset(0L, FRESH_MOVING_HIT));
        // A screen open, no fresh hit, or not grounded each reject a jump.
        assertFalse(enabled.shouldJumpReset(10L, new JumpResetContext(true, true, true, true)));
        assertFalse(enabled.shouldJumpReset(11L, new JumpResetContext(false, true, false, true)));
        assertFalse(enabled.shouldJumpReset(12L, new JumpResetContext(false, false, true, true)));
    }

    @Test
    void requireMovementGatesIdleHitsButCanBeDisabled() {
        JumpReset module = enabled();
        JumpResetContext stationaryHit = new JumpResetContext(false, true, true, false);
        assertFalse(module.shouldJumpReset(0L, stationaryHit));

        ((BooleanSetting) setting(module, "require_movement")).set(false);
        assertTrue(module.shouldJumpReset(10L, stationaryHit));
    }

    @Test
    void enforcesTheConfiguredCooldownBetweenResets() {
        JumpReset module = enabled();
        assertTrue(module.shouldJumpReset(0L, FRESH_MOVING_HIT));
        // Default cooldown is four ticks: a fresh hit inside the window is skipped.
        assertFalse(module.shouldJumpReset(2L, FRESH_MOVING_HIT));
        assertTrue(module.shouldJumpReset(4L, FRESH_MOVING_HIT));
    }

    @Test
    void disableClearsTheCooldownAndInvalidInputsAreRejected() {
        JumpReset module = enabled();
        assertTrue(module.shouldJumpReset(5L, FRESH_MOVING_HIT));
        module.disable();
        assertFalse(module.shouldJumpReset(6L, FRESH_MOVING_HIT));

        // Re-enabling starts with a cleared cooldown, so the very next fresh hit resets again.
        module.enable();
        assertTrue(module.shouldJumpReset(6L, FRESH_MOVING_HIT));

        assertThrows(IllegalArgumentException.class, () -> new JumpReset().shouldJumpReset(-1L, FRESH_MOVING_HIT));
        assertThrows(IllegalArgumentException.class, () -> new JumpReset().shouldJumpReset(0L, null));
    }

    @Test
    void accessDerivesAFreshHitFromTheHurtTimeRisingEdge() {
        JumpReset module = enabled();
        JumpResetAccess.install(module);
        JumpResetAccess.reset();

        // No prior hurt, then a rising edge (0 -> 10) is a fresh hit; a decaying edge is not.
        assertFalse(JumpResetAccess.shouldJump(false, true, true, 0));
        assertTrue(JumpResetAccess.shouldJump(false, true, true, 10));
        assertFalse(JumpResetAccess.shouldJump(false, true, true, 9));
    }

    private static JumpReset enabled() {
        JumpReset module = new JumpReset();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static dev.helikon.client.setting.Setting<?> setting(Module module, String id) {
        return module.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
