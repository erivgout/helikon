package dev.helikon.client.module.world;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedSlimeMathTest {
    @Test
    void matchesTheMinecraft262LegacyRandomFormulaIncludingCoordinateOverflow() {
        long[] seeds = {0L, 1L, 123_456_789L, -1L, 0x1234_5678_9ABCL};
        int[][] chunks = {
                {0, 0},
                {13, -27},
                {-2_048, 4_096},
                {50_000, -50_000},
                {Integer.MAX_VALUE, Integer.MIN_VALUE}
        };
        for (long seed : seeds) {
            for (int[] chunk : chunks) {
                assertEquals(vanillaReference(seed, chunk[0], chunk[1]),
                        SeedSlimeMath.isSlimeChunk(seed, chunk[0], chunk[1]));
            }
        }
    }

    @Test
    void ignoresTheUpperSixteenWorldSeedBitsLikeVanillaLegacyRandom() {
        long lower = 0x1234_5678_9ABCL;
        long withUpperBits = 0x7FFF_1234_5678_9ABCL;
        for (int chunkX = -20; chunkX <= 20; chunkX++) {
            for (int chunkZ = -20; chunkZ <= 20; chunkZ++) {
                assertEquals(SeedSlimeMath.isSlimeChunk(lower, chunkX, chunkZ),
                        SeedSlimeMath.isSlimeChunk(withUpperBits, chunkX, chunkZ));
            }
        }
    }

    private static boolean vanillaReference(long seed, int chunkX, int chunkZ) {
        long mixedSeed = seed
                + (long) (chunkX * chunkX * 4_987_142)
                + (long) (chunkX * 5_947_611)
                + (long) (chunkZ * chunkZ) * 4_392_871L
                + (long) (chunkZ * 389_711);
        return new Random(mixedSeed ^ SeedSlimeMath.SLIME_SALT).nextInt(10) == 0;
    }
}
