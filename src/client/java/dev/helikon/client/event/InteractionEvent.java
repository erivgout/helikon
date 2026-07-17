package dev.helikon.client.event;

import java.util.Objects;

/** Normal local interaction observation; it never represents a fabricated packet. */
public record InteractionEvent(Kind kind, String subjectId) implements ClientEvent {
    public enum Kind {
        ITEM_USE,
        ATTACK,
        BLOCK_BREAK,
        BLOCK_PLACE
    }

    public InteractionEvent {
        kind = Objects.requireNonNull(kind, "kind");
        subjectId = subjectId == null ? "" : subjectId.trim();
    }
}
