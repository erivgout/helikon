package dev.helikon.client.module.world;

/** Continues ordinary mining on the nearest loaded log while Attack is held. */
public final class TreeBot extends BoundedWorldAction {
    public TreeBot() {
        super("tree_bot", "TreeBot", "Mines nearby loaded logs through Minecraft's ordinary destroy path.",
                5.0D, 1);
    }

    @Override
    protected boolean accepts(Candidate candidate) {
        return candidate.log();
    }
}
