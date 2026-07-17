package dev.helikon.client.event;

import java.util.Objects;

/** Local chat-send or chat-receive observation after normal Minecraft handling begins. */
public record ChatEvent(Direction direction, String message, boolean gameMessage) implements ClientEvent {
    public enum Direction {
        SEND,
        RECEIVE
    }

    public ChatEvent {
        direction = Objects.requireNonNull(direction, "direction");
        message = Objects.requireNonNull(message, "message");
    }
}
