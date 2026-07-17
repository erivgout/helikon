package dev.helikon.client.module.world;

/** Continues ordinary mining on loaded ore blocks connected to the visible ore. */
public final class VeinMiner extends BoundedWorldAction {
    public VeinMiner() {
        super("vein_miner", "VeinMiner", "Mines loaded ore connected to the visible ore block.", 5.0D, 1);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.ore();
    }
}
