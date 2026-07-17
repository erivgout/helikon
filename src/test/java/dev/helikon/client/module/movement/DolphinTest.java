package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DolphinTest {
    private static final DolphinContext FORWARD_IN_WATER = new DolphinContext(false, true, true, true,
            false, false, false, false);

    @Test
    void requestsOrdinaryJumpOnlyForEligibleWaterMovement() {
        Dolphin dolphin = enabled(new Dolphin());

        assertEquals("dolphin", dolphin.id());
        assertEquals(ModuleCategory.MOVEMENT, dolphin.category());
        assertFalse(dolphin.defaultEnabled());
        assertTrue(dolphin.shouldJump(FORWARD_IN_WATER));
        assertFalse(dolphin.shouldJump(new DolphinContext(true, true, true, true,
                false, false, false, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, false, true, true,
                false, false, false, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, false, false,
                false, false, false, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, true, true,
                true, false, false, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, true, true,
                false, true, false, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, true, true,
                false, false, true, false)));
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, true, true,
                false, false, false, true)));
    }

    @Test
    void optionallyAllowsNonForwardWaterMovement() {
        Dolphin dolphin = enabled(new Dolphin());
        BooleanSetting forwardOnly = booleanSetting(dolphin, "forward_only");
        assertTrue(forwardOnly.value());
        assertFalse(dolphin.shouldJump(new DolphinContext(false, true, false, true,
                false, false, false, false)));

        forwardOnly.set(false);
        assertTrue(dolphin.shouldJump(new DolphinContext(false, true, false, true,
                false, false, false, false)));
    }

    private static Dolphin enabled(Dolphin module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(Dolphin module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
