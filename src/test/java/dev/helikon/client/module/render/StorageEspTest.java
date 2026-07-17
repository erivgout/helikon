package dev.helikon.client.module.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEspTest {
    @Test
    void suppliesAValidBlankCustomListAndDefaultStorageFamilies() {
        StorageEsp storageEsp = new StorageEsp();

        assertTrue(storageEsp.targetBlocks().contains("minecraft:chest"));
        assertTrue(storageEsp.targetBlocks().contains("minecraft:hopper"));
        assertTrue(storageEsp.targetBlocks().contains("minecraft:spawner"));
    }
}
