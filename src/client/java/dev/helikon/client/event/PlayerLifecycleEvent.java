package dev.helikon.client.event;

import java.util.Objects;

/** Local-player death and respawn lifecycle fact. */
public record PlayerLifecycleEvent(Phase phase) implements ClientEvent {
    public enum Phase {
        DEATH,
        RESPAWN
    }

    public PlayerLifecycleEvent {
        phase = Objects.requireNonNull(phase, "phase");
    }
}
