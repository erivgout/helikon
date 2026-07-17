package dev.helikon.client.event;

import java.util.ArrayList;
import java.util.List;

/** Converts consecutive local-player snapshots into lifecycle, motion, and inventory observations. */
public final class PlayerStateEventTracker {
    private PlayerStateSnapshot previous;
    private long inventoryRevision;

    public List<ClientEvent> observe(PlayerStateSnapshot current) {
        if (current == null) {
            previous = null;
            return List.of();
        }
        if (previous == null) {
            previous = current;
            return List.of();
        }

        List<ClientEvent> events = new ArrayList<>(4);
        if (previous.alive() && !current.alive()) {
            events.add(new PlayerLifecycleEvent(PlayerLifecycleEvent.Phase.DEATH));
        } else if (!previous.alive() && current.alive()) {
            events.add(new PlayerLifecycleEvent(PlayerLifecycleEvent.Phase.RESPAWN));
        }
        if (positionChanged(previous, current)) {
            events.add(new PlayerUpdateEvent(PlayerUpdateEvent.Kind.MOVEMENT));
        }
        if (rotationChanged(previous, current)) {
            events.add(new PlayerUpdateEvent(PlayerUpdateEvent.Kind.ROTATION));
        }
        if (previous.selectedSlot() != current.selectedSlot()
                || previous.inventoryFingerprint() != current.inventoryFingerprint()) {
            events.add(new InventoryUpdateEvent(++inventoryRevision));
        }
        previous = current;
        return List.copyOf(events);
    }

    public long inventoryRevision() {
        return inventoryRevision;
    }

    /** Clears the previous world/player baseline before a disconnect can expose replacement state. */
    public void reset() {
        previous = null;
    }

    private static boolean positionChanged(PlayerStateSnapshot previous, PlayerStateSnapshot current) {
        return Double.compare(previous.x(), current.x()) != 0
                || Double.compare(previous.y(), current.y()) != 0
                || Double.compare(previous.z(), current.z()) != 0;
    }

    private static boolean rotationChanged(PlayerStateSnapshot previous, PlayerStateSnapshot current) {
        return Float.compare(previous.yaw(), current.yaw()) != 0
                || Float.compare(previous.pitch(), current.pitch()) != 0;
    }
}
