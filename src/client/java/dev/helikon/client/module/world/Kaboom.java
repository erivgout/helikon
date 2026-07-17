package dev.helikon.client.module.world;

/** Uses a player-provided igniter on nearby loaded TNT. */
public final class Kaboom extends BoundedWorldAction {
    public Kaboom() {
        super("kaboom", "Kaboom", "Ignites nearby loaded TNT with a held vanilla ignition item.", 4.0D, 10);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.tnt();
    }
}
