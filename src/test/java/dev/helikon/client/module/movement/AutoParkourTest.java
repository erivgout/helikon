package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoParkourTest {
    private static final ParkourContext SAFE_LEDGE = new ParkourContext(false, true, true,
            0.10D, true, false, 1, true);

    @Test
    void requestsOnlySafeShallowLedgeJumpsAboveTheConfiguredSpeed() {
        AutoParkour module = new AutoParkour();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertFalse(module.shouldJump(SAFE_LEDGE));
        registry.setEnabled(module, true);
        assertTrue(module.shouldJump(SAFE_LEDGE));
        assertFalse(module.shouldJump(new ParkourContext(false, true, true, 0.07D, true, false, 1, true)));

        minimumSpeed(module).set(0.06D);
        assertTrue(module.shouldJump(new ParkourContext(false, true, true, 0.07D, true, false, 1, true)));
    }

    @Test
    void declinesScreensLavaLargeDropsAndUnsafeMovementFacts() {
        AutoParkour module = new AutoParkour();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertFalse(module.shouldJump(new ParkourContext(true, true, true, 0.10D, true, false, 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, false, true, 0.10D, true, false, 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, true, false, 0.10D, true, false, 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, true, true, 0.10D, false, false, 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, true, true, 0.10D, true, true, 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, true, true, 0.10D, true, false,
                AutoParkour.MAXIMUM_SAFE_DROP_BLOCKS + 1, true)));
        assertFalse(module.shouldJump(new ParkourContext(false, true, true, 0.10D, true, false, 1, false)));
    }

    @Test
    void rejectsMalformedMinecraftFreeFacts() {
        assertThrows(IllegalArgumentException.class, () -> new ParkourContext(false, true, true,
                Double.NaN, true, false, 1, true));
        assertThrows(IllegalArgumentException.class, () -> new ParkourContext(false, true, true,
                0.0D, true, false, -1, true));
    }

    private static NumberSetting minimumSpeed(AutoParkour module) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("minimum_movement_speed"))
                .findFirst()
                .orElseThrow();
    }
}
