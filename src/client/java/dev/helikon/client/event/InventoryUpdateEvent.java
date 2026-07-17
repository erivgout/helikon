package dev.helikon.client.event;

/** A monotonic local inventory-observation revision. */
public record InventoryUpdateEvent(long revision) implements ClientEvent {
    public InventoryUpdateEvent {
        if (revision < 0L) {
            throw new IllegalArgumentException("revision must not be negative");
        }
    }
}
