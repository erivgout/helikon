package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEspTest {
    @Test
    void highlightsOnlyConfiguredBlocksWhileEnabled() {
        BlockEsp blockEsp = new BlockEsp();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(blockEsp);

        assertFalse(blockEsp.shouldHighlight("minecraft:diamond_ore"));
        registry.setEnabled(blockEsp, true);
        assertTrue(blockEsp.shouldHighlight("minecraft:diamond_ore"));
        assertFalse(blockEsp.shouldHighlight("minecraft:stone"));
        registry.setEnabled(blockEsp, false);
        assertFalse(blockEsp.shouldHighlight("minecraft:diamond_ore"));
    }
}
