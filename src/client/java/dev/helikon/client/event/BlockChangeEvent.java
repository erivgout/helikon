package dev.helikon.client.event;

import java.util.Objects;

/** One accepted block-state change in the currently loaded client world. */
public record BlockChangeEvent(int x, int y, int z, String blockId) implements ClientEvent {
    public BlockChangeEvent {
        blockId = Objects.requireNonNull(blockId, "blockId");
    }
}
