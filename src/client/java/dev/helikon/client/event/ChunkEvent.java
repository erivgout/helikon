package dev.helikon.client.event;

import java.util.Objects;

/** Local loaded-chunk lifecycle observation. */
public record ChunkEvent(Phase phase, int chunkX, int chunkZ) implements ClientEvent {
    public enum Phase {
        LOAD,
        UNLOAD
    }

    public ChunkEvent {
        phase = Objects.requireNonNull(phase, "phase");
    }
}
