package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileThreatPolicyTest {
    private static final double HIT_RADIUS = 1.5D;
    private static final double MAXIMUM_TICKS = 40.0D;
    private static final double DETECTION_RANGE = 48.0D;

    @Test
    void warnsAboutAProjectileClosingOnTheLocalPlayer() {
        Optional<ProjectileThreatPolicy.ProjectileThreat> threat = ProjectileThreatPolicy.assess(
                0.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D, HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE);

        assertTrue(threat.isPresent());
        assertEquals(10.0D, threat.get().timeToImpactTicks(), 1.0E-9D);
        assertEquals(0.0D, threat.get().closestApproach(), 1.0E-9D);
        assertEquals(10.0D, threat.get().distance(), 1.0E-9D);
    }

    @Test
    void ignoresAProjectileMovingAway() {
        assertTrue(ProjectileThreatPolicy.assess(0.0D, 0.0D, 10.0D, 0.0D, 0.0D, 1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());
    }

    @Test
    void ignoresAProjectileThatPassesWideOfTheHitRadius() {
        assertTrue(ProjectileThreatPolicy.assess(5.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());
    }

    @Test
    void ignoresAStuckOrStationaryProjectile() {
        assertTrue(ProjectileThreatPolicy.assess(0.0D, 0.0D, 2.0D, 0.0D, 0.0D, 0.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());
    }

    @Test
    void ignoresAnImpactBeyondTheWarningLeadTime() {
        assertTrue(ProjectileThreatPolicy.assess(0.0D, 0.0D, 100.0D, 0.0D, 0.0D, -1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, 200.0D).isEmpty());
    }

    @Test
    void ignoresAProjectileBeyondTheDetectionRange() {
        assertTrue(ProjectileThreatPolicy.assess(0.0D, 0.0D, 100.0D, 0.0D, 0.0D, -1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());
    }

    @Test
    void ballisticAssessmentDetectsAnArrowThatGravityDropsIntoThePlayer() {
        assertTrue(ProjectileThreatPolicy.assess(
                0.0D, 3.0D, 10.0D, 0.0D, 0.0D, -1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());

        Optional<ProjectileThreatPolicy.ProjectileThreat> threat =
                ProjectileThreatPolicy.assessBallistic(
                        0.0D, 3.0D, 10.0D,
                        0.0D, 0.0D, -1.0D,
                        0.0D, 0.0D, 0.0D,
                        HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE,
                        0.05D, 0.99D);

        assertTrue(threat.isPresent());
        assertTrue(threat.orElseThrow().timeToImpactTicks() < 12.0D);
    }

    @Test
    void ballisticAssessmentSweepsFastPerTickSegments() {
        Optional<ProjectileThreatPolicy.ProjectileThreat> threat =
                ProjectileThreatPolicy.assessBallistic(
                        0.0D, 0.0D, 5.0D,
                        0.0D, 0.0D, -10.0D,
                        0.0D, 0.0D, 0.0D,
                        0.5D, 2.0D, DETECTION_RANGE,
                        0.05D, 0.99D);

        assertTrue(threat.isPresent());
        assertEquals(0.5D, threat.orElseThrow().timeToImpactTicks(), 1.0E-9D);
    }

    @Test
    void rejectsNonFiniteAndNonPositiveArguments() {
        assertThrows(IllegalArgumentException.class, () -> ProjectileThreatPolicy.assess(
                Double.NaN, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D, HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE));
        assertThrows(IllegalArgumentException.class, () -> ProjectileThreatPolicy.assess(
                0.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D, 0.0D, MAXIMUM_TICKS, DETECTION_RANGE));
        assertThrows(IllegalArgumentException.class, () -> ProjectileThreatPolicy.assess(
                0.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D, HIT_RADIUS, -1.0D, DETECTION_RANGE));
        assertThrows(IllegalArgumentException.class, () -> ProjectileThreatPolicy.assess(
                0.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D, HIT_RADIUS, MAXIMUM_TICKS, 0.0D));
    }

    @Test
    void rejectsANegativeThreatResult() {
        assertThrows(IllegalArgumentException.class,
                () -> new ProjectileThreatPolicy.ProjectileThreat(-1.0D, 0.0D, 0.0D));
        assertFalse(ProjectileThreatPolicy.assess(0.0D, 0.0D, 10.0D, 0.0D, 0.0D, -1.0D,
                HIT_RADIUS, MAXIMUM_TICKS, DETECTION_RANGE).isEmpty());
    }
}
