package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BreadcrumbTrailTest {
    @Test
    void samplesByDistanceAndBoundsBothAgeAndPointCount() {
        BreadcrumbTrail trail = new BreadcrumbTrail();
        trail.sample(0.0D, 64.0D, 0.0D, 1_000L, 1.0D, 2, 10_000L);
        trail.sample(0.5D, 64.0D, 0.0D, 1_001L, 1.0D, 2, 10_000L);
        trail.sample(1.0D, 64.0D, 0.0D, 1_002L, 1.0D, 2, 10_000L);
        trail.sample(2.0D, 64.0D, 0.0D, 1_003L, 1.0D, 2, 10_000L);

        assertEquals(List.of(
                new BreadcrumbTrail.Point(1.0D, 64.0D, 0.0D, 1_002L),
                new BreadcrumbTrail.Point(2.0D, 64.0D, 0.0D, 1_003L)
        ), trail.snapshot());

        trail.sample(3.0D, 64.0D, 0.0D, 20_000L, 1.0D, 2, 100L);
        assertEquals(List.of(new BreadcrumbTrail.Point(3.0D, 64.0D, 0.0D, 20_000L)), trail.snapshot());
    }

    @Test
    void clearsRatherThanMixingTimeReversedWorldStateAndRejectsUnsafeLimits() {
        BreadcrumbTrail trail = new BreadcrumbTrail();
        trail.sample(0.0D, 0.0D, 0.0D, 100L, 1.0D, 4, 1_000L);
        trail.sample(1.0D, 0.0D, 0.0D, 99L, 1.0D, 4, 1_000L);

        assertEquals(List.of(new BreadcrumbTrail.Point(1.0D, 0.0D, 0.0D, 99L)), trail.snapshot());
        assertThrows(IllegalArgumentException.class,
                () -> trail.sample(0.0D, 0.0D, 0.0D, 1L, -1.0D, 4, 1L));
    }
}
