package dev.helikon.client.module.world;

/** Places held blocks into one deterministic rotating nearby replaceable position. */
public final class BuildRandom extends BoundedWorldAction {
    public BuildRandom() {
        super("build_random", "BuildRandom", "Places held blocks at bounded rotating nearby positions while Use is held.",
                3.0D, 3);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.replaceable();
    }

    @Override
    protected double priority(Candidate candidate, long tick) {
        long hash = 31L * candidate.x() + 17L * candidate.y() + candidate.z() + tick / 3L;
        return Math.floorMod(hash * 1_103_515_245L + 12_345L, 65_521L);
    }
}
