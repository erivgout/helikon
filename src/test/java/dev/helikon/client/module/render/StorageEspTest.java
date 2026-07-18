package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEspTest {
    @Test
    void suppliesAValidBlankCustomListAndDefaultStorageFamilies() {
        StorageEsp storageEsp = new StorageEsp();

        assertTrue(storageEsp.targetBlocks().contains("minecraft:chest"));
        assertTrue(storageEsp.targetBlocks().contains("minecraft:hopper"));
        assertTrue(storageEsp.targetBlocks().contains("minecraft:spawner"));
    }

    @Test
    void highlightsOnlyEnabledSelectedLoadedBlockEntities() {
        StorageEsp storageEsp = new StorageEsp();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(storageEsp);
        registry.setEnabled(storageEsp, true);

        assertTrue(storageEsp.shouldHighlight("minecraft:chest", true));
        assertFalse(storageEsp.shouldHighlight("minecraft:chest", false));
        assertFalse(storageEsp.shouldHighlight("minecraft:stone", true));
        registry.setEnabled(storageEsp, false);
        assertFalse(storageEsp.shouldHighlight("minecraft:chest", true));
    }
}
