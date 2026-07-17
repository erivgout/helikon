package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterJumpTest {
    private static final WaterJumpContext SUITABLE_EDGE = new WaterJumpContext(false, true, true, true, true);

    @Test
    void isDisabledByDefaultAndRequiresEveryConservativeWaterEdgeFact() {
        WaterJump module = new WaterJump();
        assertFalse(module.shouldJump(SUITABLE_EDGE));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertTrue(module.shouldJump(SUITABLE_EDGE));
        assertFalse(module.shouldJump(new WaterJumpContext(true, true, true, true, true)));
        assertFalse(module.shouldJump(new WaterJumpContext(false, false, true, true, true)));
        assertFalse(module.shouldJump(new WaterJumpContext(false, true, false, true, true)));
        assertFalse(module.shouldJump(new WaterJumpContext(false, true, true, false, true)));
        assertFalse(module.shouldJump(new WaterJumpContext(false, true, true, true, false)));
    }
}
