package dev.helikon.client.module.world;

import java.util.List;

/** Exact Minecraft 26.2 legacy-random predicate used by low-altitude slime chunks. */
public final class SeedSlimeMath {
    public static final long STRUCTURE_SEED_MASK = (1L << 48) - 1L;
    public static final long SLIME_SALT = 987_234_911L;
    private static final long RANDOM_MULTIPLIER = 25_214_903_917L;
    private static final long RANDOM_ADDEND = 11L;

    private SeedSlimeMath() {
    }

    /**
     * Mirrors {@code WorldgenRandom.seedSlimeChunk(...).nextInt(10) == 0},
     * including the vanilla int overflows in the coordinate terms.
     */
    public static boolean isSlimeChunk(long structureSeed, int chunkX, int chunkZ) {
        long mixedSeed = (structureSeed & STRUCTURE_SEED_MASK)
                + (long) (chunkX * chunkX * 4_987_142)
                + (long) (chunkX * 5_947_611)
                + (long) (chunkZ * chunkZ) * 4_392_871L
                + (long) (chunkZ * 389_711);
        return nextInt10(mixedSeed ^ SLIME_SALT) == 0;
    }

    public static boolean matchesAll(long structureSeed, List<SeedCracker.Observation> observations) {
        if (observations == null || observations.isEmpty()) {
            return false;
        }
        for (SeedCracker.Observation observation : observations) {
            if (observation == null || !isSlimeChunk(
                    structureSeed, observation.coordinate().x(), observation.coordinate().z())) {
                return false;
            }
        }
        return true;
    }

    private static int nextInt10(long externalSeed) {
        long seed = (externalSeed ^ RANDOM_MULTIPLIER) & STRUCTURE_SEED_MASK;
        while (true) {
            seed = (seed * RANDOM_MULTIPLIER + RANDOM_ADDEND) & STRUCTURE_SEED_MASK;
            int bits = (int) (seed >>> 17);
            int value = bits % 10;
            if (bits - value + 9 >= 0) {
                return value;
            }
        }
    }
}
