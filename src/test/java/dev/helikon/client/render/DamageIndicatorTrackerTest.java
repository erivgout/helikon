package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DamageIndicatorTrackerTest {
    @Test
    void emitsConfirmedHealthLossThenFadesAndRisesIt() {
        DamageIndicatorTracker tracker = new DamageIndicatorTracker();
        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(7, 1.0D, 2.0D, 3.0D, 20.0F, 0)), 0L, 4, 4);
        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(7, 1.0D, 2.0D, 3.0D, 16.5F, 8)), 100L, 4, 4);

        List<DamageIndicatorTracker.RenderedIndicator> rendered = tracker.render(600L, 1_000L, 1.0D);
        assertEquals(1, rendered.size());
        DamageIndicatorTracker.RenderedIndicator indicator = rendered.getFirst();
        assertEquals(3.5F, indicator.damage());
        assertEquals(2.5D, indicator.y());
        assertEquals(0.5D, indicator.alpha());

        assertTrue(tracker.render(1_100L, 1_000L, 1.0D).isEmpty());
    }

    @Test
    void ignoresHealthChangesWithoutTheLocalHurtConfirmationAndKeepsTheNewestBoundedIndicators() {
        DamageIndicatorTracker tracker = new DamageIndicatorTracker();
        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(1, 0.0D, 0.0D, 0.0D, 20.0F, 0)), 0L, 1, 1);
        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(1, 0.0D, 0.0D, 0.0D, 18.0F, 0)), 1L, 1, 1);
        assertTrue(tracker.render(1L, 1_000L, 0.0D).isEmpty());

        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(1, 0.0D, 0.0D, 0.0D, 16.0F, 5)), 2L, 1, 1);
        tracker.observe(List.of(new DamageIndicatorTracker.ObservedEntity(1, 0.0D, 0.0D, 0.0D, 14.0F, 5)), 3L, 1, 1);

        List<DamageIndicatorTracker.RenderedIndicator> rendered = tracker.render(3L, 1_000L, 0.0D);
        assertEquals(1, rendered.size());
        assertEquals(2.0F, rendered.getFirst().damage());
    }

    @Test
    void boundsRetainedHealthSnapshotsIndependentlyOfIndicatorHistory() {
        DamageIndicatorTracker tracker = new DamageIndicatorTracker();
        List<DamageIndicatorTracker.ObservedEntity> observed = List.of(
                new DamageIndicatorTracker.ObservedEntity(1, 0.0D, 0.0D, 0.0D, 20.0F, 0),
                new DamageIndicatorTracker.ObservedEntity(2, 1.0D, 0.0D, 0.0D, 20.0F, 0),
                new DamageIndicatorTracker.ObservedEntity(3, 2.0D, 0.0D, 0.0D, 20.0F, 0)
        );

        tracker.observe(observed, 0L, 16, 2);

        assertEquals(2, tracker.trackedEntityCount());
    }
}
