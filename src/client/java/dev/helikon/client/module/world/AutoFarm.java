package dev.helikon.client.module.world;

/** Harvests the nearest mature loaded crop and requests an ordinary replant use. */
public final class AutoFarm extends BoundedWorldAction {
    public AutoFarm() {
        super("auto_farm", "AutoFarm", "Harvests and replants nearby mature loaded crops.", 4.0D, 4);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.matureCrop();
    }
}
