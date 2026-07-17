package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrajectorySimulatorTest {
    @Test
    void modelsTheVerifiedDragThenGravityArrowOrder() {
        TrajectorySimulator.Physics physics = new TrajectorySimulator.Physics(
                0.05D, 0.99D, TrajectorySimulator.GravityOrder.AFTER_DRAG, TrajectorySimulator.UpdateTiming.AFTER_MOVE
        );
        TrajectorySimulator.Result result = TrajectorySimulator.simulate(
                new TrajectoryVector(0.0D, 0.0D, 0.0D), new TrajectoryVector(1.0D, 1.0D, 0.0D),
                physics, 2, (from, to) -> Optional.empty()
        );

        assertEquals(List.of(
                new TrajectoryVector(0.0D, 0.0D, 0.0D),
                new TrajectoryVector(1.0D, 1.0D, 0.0D),
                new TrajectoryVector(1.99D, 1.94D, 0.0D)
        ), result.points());
        assertFalse(result.collided());
    }

    @Test
    void terminatesAtTheFirstInjectedCollisionAndRejectsUnsafeLimits() {
        TrajectoryVector impact = new TrajectoryVector(0.5D, 0.0D, 0.0D);
        TrajectorySimulator.Result result = TrajectorySimulator.simulate(
                new TrajectoryVector(0.0D, 0.0D, 0.0D), new TrajectoryVector(1.0D, 0.0D, 0.0D),
                new TrajectorySimulator.Physics(0.03D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG,
                        TrajectorySimulator.UpdateTiming.BEFORE_MOVE),
                5, (from, to) -> Optional.of(impact)
        );

        assertEquals(List.of(new TrajectoryVector(0.0D, 0.0D, 0.0D), impact), result.points());
        assertTrue(result.collided());
        assertThrows(IllegalArgumentException.class, () -> TrajectorySimulator.simulate(
                impact, impact, new TrajectorySimulator.Physics(0.0D, 1.0D,
                        TrajectorySimulator.GravityOrder.AFTER_DRAG, TrajectorySimulator.UpdateTiming.AFTER_MOVE),
                0, (from, to) -> Optional.empty()));
    }

    @Test
    void supportsTheVerifiedGravityBeforeDragThrownItemOrder() {
        TrajectorySimulator.Physics physics = new TrajectorySimulator.Physics(
                0.05D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG, TrajectorySimulator.UpdateTiming.BEFORE_MOVE
        );
        assertEquals(new TrajectoryVector(0.99D, 0.9405D, 0.0D),
                physics.nextVelocity(new TrajectoryVector(1.0D, 1.0D, 0.0D)));
    }

    @Test
    void streamsRenderSegmentsWithoutBuildingAPointCollection() {
        java.util.ArrayList<String> segments = new java.util.ArrayList<>();
        TrajectorySimulator.TraceResult result = TrajectorySimulator.trace(
                new TrajectoryVector(0.0D, 0.0D, 0.0D), new TrajectoryVector(1.0D, 0.0D, 0.0D),
                new TrajectorySimulator.Physics(0.0D, 1.0D, TrajectorySimulator.GravityOrder.AFTER_DRAG,
                        TrajectorySimulator.UpdateTiming.AFTER_MOVE), 2,
                (from, to) -> Optional.empty(), (from, to) -> segments.add(from + "->" + to)
        );

        assertEquals(2, segments.size());
        assertFalse(result.collided());
        assertEquals(new TrajectoryVector(2.0D, 0.0D, 0.0D), result.terminalPoint());
    }

    @Test
    void appliesThrownProjectilePhysicsBeforeItsFirstMovement() {
        TrajectorySimulator.Result result = TrajectorySimulator.simulate(
                new TrajectoryVector(0.0D, 0.0D, 0.0D), new TrajectoryVector(1.0D, 1.0D, 0.0D),
                new TrajectorySimulator.Physics(0.05D, 0.99D, TrajectorySimulator.GravityOrder.BEFORE_DRAG,
                        TrajectorySimulator.UpdateTiming.BEFORE_MOVE),
                1, (from, to) -> Optional.empty()
        );

        assertEquals(new TrajectoryVector(0.99D, 0.9405D, 0.0D), result.points().get(1));
    }
}
