package dev.helikon.client.event;

import java.util.Objects;

/** Lifecycle fact for the currently connected local world. */
public record WorldEvent(Phase phase, String serverAddress) implements ClientEvent {
    public enum Phase {
        JOIN,
        LEAVE
    }

    public WorldEvent {
        phase = Objects.requireNonNull(phase, "phase");
        serverAddress = serverAddress == null ? "" : serverAddress.trim();
    }
}
