package dev.helikon.client.module.world;

/** Tracks whether an ordinary survival destroy target must be started or continued. */
final class NukerBreakSequence {
    enum Action {
        START,
        CONTINUE
    }

    private long activePosition = Long.MIN_VALUE;

    Action next(long position) {
        if (position != activePosition) {
            activePosition = position;
            return Action.START;
        }
        return Action.CONTINUE;
    }

    void reset() {
        activePosition = Long.MIN_VALUE;
    }
}
