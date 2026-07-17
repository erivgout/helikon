package dev.helikon.client.module.world;

/** Uses a held hoe on the nearest loaded tillable block. */
public final class TillAura extends BoundedWorldAction {
    public TillAura() {
        super("till_aura", "TillAura", "Uses a held hoe on nearby loaded dirt or grass.", 4.0D, 3);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.tillable();
    }
}
