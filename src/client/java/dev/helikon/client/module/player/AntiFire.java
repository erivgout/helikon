package dev.helikon.client.module.player;

/** Uses an existing water bucket or fire-resistance potion while burning. */
public final class AntiFire extends HotbarUseModule {
    public AntiFire() {
        super("anti_fire", "AntiFire", "Uses a held hotbar fire countermeasure while the local player is burning.", 20);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.waterBucket() || candidate.fireResistance();
    }
}
