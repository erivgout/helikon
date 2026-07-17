package dev.helikon.client.event;

import java.util.Objects;

/** Resource-reload lifecycle boundary. */
public record ResourceReloadEvent(Phase phase) implements ClientEvent {
    public enum Phase {
        START,
        COMPLETE
    }

    public ResourceReloadEvent {
        phase = Objects.requireNonNull(phase, "phase");
    }
}
