package dev.helikon.client.chat;

import dev.helikon.client.module.chat.AnnouncementTrigger;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnnouncerObservationTrackerTest {
    @Test
    void emitsOnlyThresholdCrossingsAndResetsDistanceAcrossDimensions() {
        AnnouncerObservationTracker tracker = new AnnouncerObservationTracker();
        assertTrue(tracker.observe(fact(0, 20.0F, "minecraft:overworld"), 10, 6.0F).isEmpty());

        assertEquals(List.of(new AnnouncerObservationTracker.Observation(
                AnnouncementTrigger.DISTANCE_TRAVELED, "10 blocks")),
                tracker.observe(fact(11, 20.0F, "minecraft:overworld"), 10, 6.0F));
        assertEquals(List.of(new AnnouncerObservationTracker.Observation(
                AnnouncementTrigger.LOW_HEALTH, "5.5 health")),
                tracker.observe(fact(11, 5.5F, "minecraft:overworld"), 10, 6.0F));
        assertEquals(List.of(new AnnouncerObservationTracker.Observation(
                AnnouncementTrigger.DIMENSION_CHANGE, "minecraft:the_nether")),
                tracker.observe(fact(1_000, 5.5F, "minecraft:the_nether"), 10, 6.0F));
    }

    @Test
    void rejectsMalformedLocalFactsAndThresholds() {
        AnnouncerObservationTracker tracker = new AnnouncerObservationTracker();
        assertThrows(IllegalArgumentException.class,
                () -> new AnnouncerObservationTracker.Fact(Double.NaN, 0, 0, 20.0F, "minecraft:overworld"));
        assertThrows(IllegalArgumentException.class,
                () -> tracker.observe(fact(0, 20.0F, "minecraft:overworld"), 0, 6.0F));
    }

    private static AnnouncerObservationTracker.Fact fact(double x, float health, String dimension) {
        return new AnnouncerObservationTracker.Fact(x, 64.0D, 0.0D, health, dimension);
    }
}
