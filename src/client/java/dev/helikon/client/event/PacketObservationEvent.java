package dev.helikon.client.event;

import java.util.Objects;

/** Metadata-only packet boundary observation for future adapters; no packet object is exposed. */
public record PacketObservationEvent(Direction direction, String packetType) implements ClientEvent {
    public enum Direction {
        SEND,
        RECEIVE
    }

    public PacketObservationEvent {
        direction = Objects.requireNonNull(direction, "direction");
        packetType = requireText(packetType, "packetType");
    }

    private static String requireText(String value, String field) {
        String nonNull = Objects.requireNonNull(value, field).trim();
        if (nonNull.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return nonNull;
    }
}
