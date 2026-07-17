package dev.helikon.client.module.world;

/** Mines blocks in a bounded two-high corridor in the current view direction. */
public final class Tunneller extends BoundedWorldAction {
    public Tunneller() {
        super("tunneller", "Tunneller", "Mines a bounded two-high corridor in the current view direction.",
                5.0D, 1);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.tunnel();
    }
}
