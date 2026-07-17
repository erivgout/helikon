package dev.helikon.client.event;

import java.util.Objects;

/** Local render lifecycle fact for HUD, world, entity, and block-outline adapters. */
public record RenderEvent(Kind kind, double tickProgress, String subjectId) implements ClientEvent {
    public enum Kind {
        HUD,
        WORLD,
        ENTITY,
        BLOCK_OUTLINE
    }

    public RenderEvent {
        kind = Objects.requireNonNull(kind, "kind");
        if (!Double.isFinite(tickProgress) || tickProgress < 0.0D) {
            throw new IllegalArgumentException("tickProgress must be finite and non-negative");
        }
        subjectId = subjectId == null ? "" : subjectId.trim();
    }
}
