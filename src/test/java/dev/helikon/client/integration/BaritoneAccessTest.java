package dev.helikon.client.integration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaritoneAccessTest {
    @Test
    void inventoryAutomationRequiresTheHelikonModuleToBeActive() {
        BaritoneAccess.Options enabled = options(true, true);
        BaritoneAccess.Options disabled = options(false, true);

        assertTrue(enabled.inventoryAutomationEnabled());
        assertFalse(disabled.inventoryAutomationEnabled(),
                "panic must not leave Baritone moving tools or block stacks into the hotbar");
        assertFalse(options(true, false).inventoryAutomationEnabled());
    }

    private static BaritoneAccess.Options options(boolean active, boolean allowInventory) {
        return new BaritoneAccess.Options(active, true, true, true, false, allowInventory,
                true, true, true);
    }
}
