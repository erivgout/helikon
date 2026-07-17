package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryWalkTest {
    private static final MovementInput CURRENT = new MovementInput(false, false, false, false,
            false, true, false);
    private static final MovementInput PHYSICAL = new MovementInput(true, false, true, false,
            true, true, true);

    @Test
    void addsOnlyPhysicalMovementKeysInAnUnfocusedPlayerInventory() {
        InventoryWalk module = enabledModule();

        assertEquals(new MovementInput(true, false, true, false, true, true, true),
                module.apply(CURRENT, PHYSICAL, true, false));
    }

    @Test
    void staysInactiveOutsideInventoryAndWhileAWidgetAcceptsText() {
        InventoryWalk module = enabledModule();

        assertEquals(CURRENT, module.apply(CURRENT, PHYSICAL, false, false));
        assertEquals(CURRENT, module.apply(CURRENT, PHYSICAL, true, true));
    }

    private static InventoryWalk enabledModule() {
        InventoryWalk module = new InventoryWalk();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
