package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowProjectionTest {
    private static final double YAW_SOUTH = 0.0D;
    private static final double PITCH_LEVEL = 0.0D;
    private static final double FOV = 70.0D;

    @Test
    void targetInsideTheConeIsNotMarked() {
        // Facing south (+Z), a target 5 blocks straight ahead is within the cone.
        ArrowProjection.Result result = ArrowProjection.project(0.0D, 0.0D, 5.0D, YAW_SOUTH, PITCH_LEVEL, FOV);
        assertFalse(result.outside());
    }

    @Test
    void targetToThePlayersRightPointsRightOnScreen() {
        // Facing south, the player's right hand points west (-X); a target there is outside the cone.
        ArrowProjection.Result result = ArrowProjection.project(-3.0D, 0.0D, 0.0D, YAW_SOUTH, PITCH_LEVEL, FOV);
        assertTrue(result.outside());
        assertEquals(1.0D, result.directionX(), 1.0e-6D);
        assertEquals(0.0D, result.directionY(), 1.0e-6D);
        assertEquals(3.0D, result.distance(), 1.0e-6D);
    }

    @Test
    void targetToThePlayersLeftPointsLeftOnScreen() {
        ArrowProjection.Result result = ArrowProjection.project(3.0D, 0.0D, 0.0D, YAW_SOUTH, PITCH_LEVEL, FOV);
        assertTrue(result.outside());
        assertEquals(-1.0D, result.directionX(), 1.0e-6D);
        assertEquals(0.0D, result.directionY(), 1.0e-6D);
    }

    @Test
    void targetAbovePointsUpOnScreen() {
        ArrowProjection.Result result = ArrowProjection.project(0.0D, 3.0D, 0.0D, YAW_SOUTH, PITCH_LEVEL, FOV);
        assertTrue(result.outside());
        assertEquals(0.0D, result.directionX(), 1.0e-6D);
        assertEquals(-1.0D, result.directionY(), 1.0e-6D);
    }

    @Test
    void targetDirectlyBehindPointsDown() {
        ArrowProjection.Result result = ArrowProjection.project(0.0D, 0.0D, -5.0D, YAW_SOUTH, PITCH_LEVEL, FOV);
        assertTrue(result.outside());
        assertEquals(0.0D, result.directionX(), 1.0e-9D);
        assertEquals(1.0D, result.directionY(), 1.0e-9D);
    }

    @Test
    void widerFieldOfViewSuppressesNearCenterTargets() {
        // A target 40 degrees off center: inside a 100-degree cone, outside a 60-degree cone.
        double angle = Math.toRadians(40.0D);
        double deltaX = -Math.sin(angle);
        double deltaZ = Math.cos(angle);
        assertFalse(ArrowProjection.project(deltaX, 0.0D, deltaZ, YAW_SOUTH, PITCH_LEVEL, 100.0D).outside());
        assertTrue(ArrowProjection.project(deltaX, 0.0D, deltaZ, YAW_SOUTH, PITCH_LEVEL, 60.0D).outside());
    }

    @Test
    void invalidInputIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> ArrowProjection.project(Double.NaN, 0.0D, 0.0D, YAW_SOUTH, PITCH_LEVEL, FOV));
        assertThrows(IllegalArgumentException.class,
                () -> ArrowProjection.project(0.0D, 0.0D, 1.0D, YAW_SOUTH, PITCH_LEVEL, 0.0D));
    }
}
