package dev.helikon.client.module.world;

/** Uses a held bucket on the nearest compatible loaded liquid position. */
public final class Liquids extends BoundedWorldAction {
    public Liquids() {
        super("liquids", "Liquids", "Uses the held bucket on nearby loaded liquid sources or replaceable positions.",
                4.0D, 6);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.liquidSource() || candidate.replaceable();
    }
}
