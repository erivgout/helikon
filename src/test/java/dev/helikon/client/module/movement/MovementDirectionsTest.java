package dev.helikon.client.module.movement;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovementDirectionsTest {
    @Test
    void positiveSideInputAlwaysMovesLeftOfTheViewDirection() {
        // Facing south (+Z): left is east (+X), right is west (-X).
        assertEquals(new HorizontalVelocity(1.0D, 0.0D),
                MovementDirections.fromView(0.0D, 1.0D, 1.0D, 0.0D));
        assertEquals(new HorizontalVelocity(-1.0D, 0.0D),
                MovementDirections.fromView(0.0D, 1.0D, -1.0D, 0.0D));

        // Facing east (+X): left is north (-Z).
        assertEquals(new HorizontalVelocity(0.0D, -1.0D),
                MovementDirections.fromView(1.0D, 0.0D, 1.0D, 0.0D));
    }

    @Test
    void forwardAndDiagonalInputRetainTheirExpectedAxes() {
        assertEquals(new HorizontalVelocity(0.0D, 1.0D),
                MovementDirections.fromView(0.0D, 1.0D, 0.0D, 1.0D));
        assertEquals(new HorizontalVelocity(1.0D, 1.0D),
                MovementDirections.fromView(0.0D, 1.0D, 1.0D, 1.0D));
    }
}
