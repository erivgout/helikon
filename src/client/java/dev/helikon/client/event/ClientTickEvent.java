package dev.helikon.client.event;

/** Published around the Fabric client tick hooks. */
public record ClientTickEvent(Phase phase) implements ClientEvent {
    public enum Phase {
        PRE,
        POST
    }
}
