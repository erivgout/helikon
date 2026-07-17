package dev.helikon.client.module.world;

/** Continues ordinary mining across a bounded plane matching the crosshair block. */
public final class Excavator extends BoundedWorldAction {
    public Excavator() {
        super("excavator", "Excavator", "Mines a bounded plane of blocks matching the visible crosshair block.",
                3.0D, 1);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.excavatable();
    }
}
