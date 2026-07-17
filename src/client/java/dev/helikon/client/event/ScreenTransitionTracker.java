package dev.helikon.client.event;

import java.util.List;
import java.util.Objects;

/** Minecraft-free identity tracker that turns adapter screen replacements into lifecycle events. */
public final class ScreenTransitionTracker {
    private Object activeToken;
    private String activeId = "";

    public List<ScreenEvent> update(Object nextToken, String nextId) {
        if (nextToken == activeToken) {
            return List.of();
        }
        if (nextToken == null && nextId != null && !nextId.isBlank()) {
            throw new IllegalArgumentException("An absent screen token cannot have an ID");
        }

        List<ScreenEvent> transitions = activeToken == null
                ? List.of()
                : List.of(new ScreenEvent(ScreenEvent.Phase.CLOSE, activeId));
        activeToken = nextToken;
        activeId = nextToken == null ? "" : requireId(nextId);
        if (activeToken == null) {
            return transitions;
        }
        return transitions.isEmpty()
                ? List.of(new ScreenEvent(ScreenEvent.Phase.OPEN, activeId))
                : List.of(transitions.getFirst(), new ScreenEvent(ScreenEvent.Phase.OPEN, activeId));
    }

    private static String requireId(String value) {
        String nonNull = Objects.requireNonNull(value, "nextId").trim();
        if (nonNull.isEmpty()) {
            throw new IllegalArgumentException("nextId must not be blank for a screen token");
        }
        return nonNull;
    }
}
