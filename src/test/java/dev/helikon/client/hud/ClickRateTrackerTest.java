package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClickRateTrackerTest {
    @Test
    void countsButtonsIndependentlyInsideRollingSecond() {
        ClickRateTracker tracker = new ClickRateTracker();
        tracker.record(ClickRateTracker.Button.LEFT, 0L);
        tracker.record(ClickRateTracker.Button.LEFT, 100_000_000L);
        tracker.record(ClickRateTracker.Button.RIGHT, 900_000_000L);

        assertEquals(2, tracker.clicksPerSecond(ClickRateTracker.Button.LEFT, 999_999_999L));
        assertEquals(1, tracker.clicksPerSecond(ClickRateTracker.Button.RIGHT, 999_999_999L));
        assertEquals(1, tracker.clicksPerSecond(ClickRateTracker.Button.LEFT, 1_000_000_000L));
        assertEquals(0, tracker.clicksPerSecond(ClickRateTracker.Button.LEFT, 1_100_000_000L));
        assertEquals(1, tracker.clicksPerSecond(ClickRateTracker.Button.RIGHT, 1_100_000_000L));
    }

    @Test
    void clearRemovesBothCounts() {
        ClickRateTracker tracker = new ClickRateTracker();
        tracker.record(ClickRateTracker.Button.LEFT, -500_000_000L);
        tracker.record(ClickRateTracker.Button.RIGHT, -500_000_000L);

        tracker.clear();

        assertEquals(0, tracker.clicksPerSecond(ClickRateTracker.Button.LEFT, 0L));
        assertEquals(0, tracker.clicksPerSecond(ClickRateTracker.Button.RIGHT, 0L));
    }
}
