package dev.helikon.client.module.world;

/** Applies player-provided bone meal to the nearest loaded growable block. */
public final class BonemealAura extends BoundedWorldAction {
    public BonemealAura() {
        super("bonemeal_aura", "BonemealAura", "Uses held bone meal on nearby loaded growable blocks.", 4.0D, 4);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.growable();
    }
}
