package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockEspPolicyTest {
    @Test
    void parsesOnlyBoundedCanonicalBlockIdentifiers() {
        assertEquals(java.util.Set.of("minecraft:diamond_ore", "example:folder/block"),
                BlockIdList.parse(" Minecraft:Diamond_Ore ; invalid id ; example:folder/block ; minecraft:diamond_ore"));
        assertTrue(BlockIdList.parse(" ").isEmpty());
    }

    @Test
    void cursorScansInclusiveCubeAndRestartsAfterItsLastCoordinate() {
        BlockEspScanCursor cursor = new BlockEspScanCursor();
        BlockEspScanCursor.Region region = new BlockEspScanCursor.Region(0, 10, 0, 1, 0);
        List<BlockEspScanCursor.Position> positions = new ArrayList<>();
        for (int index = 0; index < 10; index++) {
            positions.add(cursor.next(region));
        }

        assertEquals(new BlockEspScanCursor.Position(-1, 10, -1), positions.getFirst());
        assertEquals(new BlockEspScanCursor.Position(1, 10, 1), positions.get(8));
        assertEquals(new BlockEspScanCursor.Position(-1, 10, -1), positions.get(9));
    }

    @Test
    void cacheRemovesNonMatchesAndEvictsTheOldestBoundedEntry() {
        BlockEspCache cache = new BlockEspCache(2);
        BlockEspScanCursor.Position first = new BlockEspScanCursor.Position(1, 2, 3);
        BlockEspScanCursor.Position second = new BlockEspScanCursor.Position(4, 5, 6);
        BlockEspScanCursor.Position third = new BlockEspScanCursor.Position(7, 8, 9);
        cache.observe(first, true);
        cache.observe(second, true);
        cache.observe(third, true);
        cache.observe(second, false);
        cache.retain(position -> position.equals(third));

        List<BlockEspScanCursor.Position> retained = new ArrayList<>();
        cache.positions().forEach(retained::add);
        assertEquals(List.of(third), retained);
        assertEquals(1, cache.size());
        assertFalse(retained.contains(first));
    }

    @Test
    void rangeCullRejectsEntriesRetainedFromAnOlderNearbyScanRegion() {
        BlockEspScanCursor.Position oldResult = new BlockEspScanCursor.Position(0, 64, 0);

        assertTrue(BlockEspRange.contains(oldResult, 0.0D, 64.0D, 0.0D, 24, 24));
        assertFalse(BlockEspRange.contains(oldResult, 100.0D, 64.0D, 0.0D, 24, 24));
    }

    @Test
    void disabledOrEmptyConfiguredListsDoNotScheduleWorldScanning() {
        assertFalse(BlockEspScanPolicy.shouldScan(false, java.util.Set.of("minecraft:diamond_ore")));
        assertFalse(BlockEspScanPolicy.shouldScan(true, java.util.Set.of()));
        assertTrue(BlockEspScanPolicy.shouldScan(true, java.util.Set.of("minecraft:diamond_ore")));
    }
}
