package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StorageEspTargetsTest {
    @Test
    void resolvesSelectedStorageFamiliesAndValidatedCustomEntries() {
        Set<String> targets = StorageEspTargets.resolve(true, false, false, false, true, false,
                "minecraft:beacon; invalid identifier ; minecraft:chest");

        assertTrue(targets.contains("minecraft:chest"));
        assertTrue(targets.contains("minecraft:trapped_chest"));
        assertTrue(targets.contains("minecraft:hopper"));
        assertTrue(targets.contains("minecraft:beacon"));
        assertFalse(targets.contains("minecraft:barrel"));
        assertEquals(5, targets.size());
    }

    @Test
    void permitsAnEmptyTargetSetWhenEveryCategoryIsDisabled() {
        assertTrue(StorageEspTargets.resolve(false, false, false, false, false, false, " ").isEmpty());
    }
}
