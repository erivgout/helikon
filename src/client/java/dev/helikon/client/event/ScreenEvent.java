package dev.helikon.client.event;

import java.util.Objects;

/** Open/close transition for a local screen, represented without a Minecraft class reference. */
public record ScreenEvent(Phase phase, String screenId) implements ClientEvent {
    public enum Phase {
        OPEN,
        CLOSE
    }

    public ScreenEvent {
        phase = Objects.requireNonNull(phase, "phase");
        screenId = requireText(screenId, "screenId");
    }

    private static String requireText(String value, String field) {
        String nonNull = Objects.requireNonNull(value, field).trim();
        if (nonNull.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return nonNull;
    }
}
