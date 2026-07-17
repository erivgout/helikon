package dev.helikon.client.event;

import java.util.Objects;

/** Local player movement or rotation update boundary. */
public record PlayerUpdateEvent(Kind kind) implements ClientEvent {
    public enum Kind {
        MOVEMENT,
        ROTATION
    }

    public PlayerUpdateEvent {
        kind = Objects.requireNonNull(kind, "kind");
    }
}
