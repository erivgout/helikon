package dev.helikon.client.module.world;

import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedCrackerTest {
    @Test
    void exposesBoundedProductionDefaultsAndCleanLifecycle() {
        SeedCracker module = new SeedCracker();

        assertEquals("seed_cracker", module.id());
        assertEquals("SeedCracker", module.name());
        assertEquals(4, integerSetting(module, "minimum_observations").value());
        assertEquals(1_000_000, integerSetting(module, "search_count").value());
        assertEquals(10_000, integerSetting(module, "candidates_per_tick").value());
        assertTrue(booleanSetting(module, "automatic_slimes").value());
        assertTrue(booleanSetting(module, "show_hud").value());
        assertEquals(SeedCracker.State.DISABLED, module.snapshot().state());

        module.enable();
        assertEquals(SeedCracker.State.COLLECTING, module.snapshot().state());
        module.disable();
        assertEquals(SeedCracker.State.DISABLED, module.snapshot().state());
    }

    @Test
    void distinctSlimesConfirmAChunkAndDuplicateEntitiesDoNot() {
        SeedCracker module = new SeedCracker();
        integerSetting(module, "confirmations_per_chunk").set(2);
        module.enable();
        UUID first = UUID.randomUUID();

        assertFalse(module.observeSlime(first, 4, -7, 10L));
        assertFalse(module.observeSlime(first, 4, -7, 11L));
        assertTrue(module.observeSlime(UUID.randomUUID(), 4, -7, 12L));
        assertEquals(1, module.observations().size());
        assertEquals(2, module.observations().getFirst().confirmations());
    }

    @Test
    void incrementallyFindsTheKnownStructureSeedInsideTheConfiguredRange() {
        long expectedSeed = 123_456L;
        SeedCracker module = new SeedCracker();
        booleanSetting(module, "automatic_search").set(false);
        stringSetting(module, "search_start").set("123000");
        integerSetting(module, "search_count").set(2_000);
        integerSetting(module, "candidates_per_tick").set(1_000);
        knownSlimeChunks(expectedSeed, 6).forEach(chunk ->
                module.addManualObservation(chunk.x(), chunk.z()));
        module.enable();

        assertTrue(module.requestSearch());
        module.tick();
        assertEquals(1_000L, module.snapshot().scannedCandidates());
        assertEquals(SeedCracker.State.SEARCHING, module.snapshot().state());
        module.tick();

        assertEquals(SeedCracker.State.COMPLETE, module.snapshot().state());
        assertEquals(2_000L, module.snapshot().scannedCandidates());
        assertTrue(module.snapshot().candidates().contains(expectedSeed));
    }

    @Test
    void singleplayerRevealProducesTheFullAndLowerFortyEightBitSeed() {
        SeedCracker module = new SeedCracker();
        module.enable();
        long worldSeed = -7_654_321_234_567L;

        module.revealLocalWorldSeed(worldSeed);

        assertEquals(SeedCracker.State.SOLVED, module.snapshot().state());
        assertEquals(worldSeed, module.snapshot().fullWorldSeed());
        assertEquals(worldSeed & SeedSlimeMath.STRUCTURE_SEED_MASK,
                module.snapshot().candidates().getFirst());
    }

    @Test
    void solvedSeedRemainsConsistentWhenEvidenceChangesAndIsReacquiredAfterDisable() {
        SeedCracker module = new SeedCracker();
        module.enable();
        long worldSeed = 987_654_321L;
        module.revealLocalWorldSeed(worldSeed);

        assertTrue(module.addManualObservation(0, 0));
        assertEquals(SeedCracker.State.SOLVED, module.snapshot().state());
        assertEquals(worldSeed & SeedSlimeMath.STRUCTURE_SEED_MASK,
                module.snapshot().candidates().getFirst());

        module.disable();
        assertEquals(SeedCracker.State.DISABLED, module.snapshot().state());
        assertEquals(null, module.snapshot().fullWorldSeed());
        module.enable();
        assertEquals(SeedCracker.State.COLLECTING, module.snapshot().state());
        module.revealLocalWorldSeed(worldSeed);
        assertEquals(SeedCracker.State.SOLVED, module.snapshot().state());
    }

    @Test
    void rejectsMalformedRangesAndSupportsManualEvidenceCorrection() {
        SeedCracker module = new SeedCracker();
        booleanSetting(module, "automatic_search").set(false);
        integerSetting(module, "minimum_observations").set(1);
        module.addManualObservation(2, 3);
        module.enable();
        stringSetting(module, "search_start").set("not-a-seed");

        assertFalse(module.requestSearch());
        assertEquals(SeedCracker.State.ERROR, module.snapshot().state());
        assertFalse(module.snapshot().error().isBlank());
        assertTrue(module.removeObservation(2, 3));
        assertEquals(SeedCracker.State.COLLECTING, module.snapshot().state());
    }

    @Test
    void disabledModuleCannotAnnounceASearchThatWillNotAdvance() {
        SeedCracker module = new SeedCracker();
        integerSetting(module, "minimum_observations").set(1);
        module.addManualObservation(2, 3);

        assertFalse(module.requestSearch());
        assertEquals(SeedCracker.State.DISABLED, module.snapshot().state());
        assertEquals(0L, module.snapshot().rangeCount());
    }

    @Test
    void clearSessionRemovesWorldSpecificEvidenceResultsAndEntityDeduplication() {
        SeedCracker module = new SeedCracker();
        integerSetting(module, "minimum_observations").set(1);
        module.enable();
        UUID slime = UUID.randomUUID();
        assertTrue(module.observeSlime(slime, 1, 1, 1L));

        module.clearSession();

        assertTrue(module.observations().isEmpty());
        assertTrue(module.snapshot().candidates().isEmpty());
        assertEquals(SeedCracker.State.COLLECTING, module.snapshot().state());
        assertTrue(module.observeSlime(slime, 1, 1, 2L));
    }

    private static List<SeedCracker.ChunkCoordinate> knownSlimeChunks(long seed, int count) {
        List<SeedCracker.ChunkCoordinate> chunks = new ArrayList<>();
        for (int x = -100; x <= 100 && chunks.size() < count; x++) {
            for (int z = -100; z <= 100 && chunks.size() < count; z++) {
                if (SeedSlimeMath.isSlimeChunk(seed, x, z)) {
                    chunks.add(new SeedCracker.ChunkCoordinate(x, z));
                }
            }
        }
        return List.copyOf(chunks);
    }

    private static BooleanSetting booleanSetting(SeedCracker module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integerSetting(SeedCracker module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static StringSetting stringSetting(SeedCracker module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
