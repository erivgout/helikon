package dev.helikon.client.module.world;

/** Interacts with the second loaded solid block along the local view ray. */
public final class GhostHand extends BoundedWorldAction {
    public GhostHand() {
        super("ghost_hand", "GhostHand", "Uses the held item on a loaded block behind the first visible obstruction.",
                5.0D, 4);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.excavatable();
    }
}
