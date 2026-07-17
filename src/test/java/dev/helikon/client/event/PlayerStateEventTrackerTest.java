package dev.helikon.client.event;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PlayerStateEventTrackerTest {
    @Test
    void emitsOnlyChangedLifecycleMotionRotationAndInventoryFacts() {
        PlayerStateEventTracker tracker = new PlayerStateEventTracker();
        PlayerStateSnapshot initial = snapshot(true, 1.0D, 2.0D, 3.0D, 10.0F, 20.0F, 0, 4L);

        assertEquals(List.of(), tracker.observe(initial));
        assertEquals(List.of(), tracker.observe(initial));
        assertEquals(List.of(
                new PlayerUpdateEvent(PlayerUpdateEvent.Kind.MOVEMENT),
                new PlayerUpdateEvent(PlayerUpdateEvent.Kind.ROTATION),
                new InventoryUpdateEvent(1L)
        ), tracker.observe(snapshot(true, 2.0D, 2.0D, 3.0D, 15.0F, 20.0F, 0, 5L)));
        assertEquals(List.of(new InventoryUpdateEvent(2L)),
                tracker.observe(snapshot(true, 2.0D, 2.0D, 3.0D, 15.0F, 20.0F, 1, 5L)));
        assertEquals(List.of(new PlayerLifecycleEvent(PlayerLifecycleEvent.Phase.DEATH)),
                tracker.observe(snapshot(false, 2.0D, 2.0D, 3.0D, 15.0F, 20.0F, 1, 5L)));
        assertEquals(List.of(new PlayerLifecycleEvent(PlayerLifecycleEvent.Phase.RESPAWN)),
                tracker.observe(snapshot(true, 2.0D, 2.0D, 3.0D, 15.0F, 20.0F, 1, 5L)));
        assertEquals(2L, tracker.inventoryRevision());
    }

    @Test
    void worldAbsenceResetsTheBaselineWithoutProducingSpuriousLifecycleEvents() {
        PlayerStateEventTracker tracker = new PlayerStateEventTracker();
        tracker.observe(snapshot(false, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0, 0L));
        assertEquals(List.of(), tracker.observe(null));
        assertEquals(List.of(), tracker.observe(snapshot(true, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, 0, 0L)));
        tracker.reset();
        assertEquals(List.of(), tracker.observe(snapshot(true, 8.0D, 8.0D, 8.0D, 0.0F, 0.0F, 0, 4L)));
    }

    @Test
    void snapshotRejectsNonFiniteCoordinatesAndRotation() {
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(true, Double.NaN, 0.0D, 0.0D, 0.0F, 0.0F, 0, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(true, 0.0D, 0.0D, 0.0D, Float.NaN, 0.0F, 0, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(true, 0.0D, 0.0D, 0.0D, 0.0F, 0.0F, -1, 0L));
    }

    private static PlayerStateSnapshot snapshot(boolean alive, double x, double y, double z,
                                                float yaw, float pitch, int selectedSlot, long inventoryFingerprint) {
        return new PlayerStateSnapshot(alive, x, y, z, yaw, pitch, selectedSlot, inventoryFingerprint);
    }
}
