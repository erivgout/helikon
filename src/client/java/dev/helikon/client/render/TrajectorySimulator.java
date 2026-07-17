package dev.helikon.client.render;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Deterministic local projectile path calculation with an injected world-collision boundary. */
public final class TrajectorySimulator {
    private TrajectorySimulator() {
    }

    public static Result simulate(TrajectoryVector start, TrajectoryVector initialVelocity, Physics physics,
                                  int maximumSteps, CollisionDetector collisionDetector) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(initialVelocity, "initialVelocity");
        Objects.requireNonNull(physics, "physics");
        Objects.requireNonNull(collisionDetector, "collisionDetector");
        validateMaximumSteps(maximumSteps);

        List<TrajectoryVector> points = new ArrayList<>(maximumSteps + 1);
        points.add(start);
        TrajectoryVector position = start;
        TrajectoryVector velocity = initialVelocity;
        for (int step = 0; step < maximumSteps; step++) {
            if (physics.updateTiming() == UpdateTiming.BEFORE_MOVE) {
                velocity = physics.nextVelocity(velocity);
            }
            TrajectoryVector next = position.add(velocity);
            Optional<TrajectoryVector> collision = collisionDetector.firstCollision(position, next);
            if (collision.isPresent()) {
                points.add(collision.get());
                return new Result(List.copyOf(points), true);
            }
            points.add(next);
            position = next;
            if (physics.updateTiming() == UpdateTiming.AFTER_MOVE) {
                velocity = physics.nextVelocity(velocity);
            }
        }
        return new Result(List.copyOf(points), false);
    }

    /**
     * Streams one segment at a time for render paths, avoiding a per-frame point collection.
     * The returned terminal point is the collision point when {@code collided} is true.
     */
    public static TraceResult trace(TrajectoryVector start, TrajectoryVector initialVelocity, Physics physics,
                                    int maximumSteps, CollisionDetector collisionDetector, SegmentConsumer segmentConsumer) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(initialVelocity, "initialVelocity");
        Objects.requireNonNull(physics, "physics");
        Objects.requireNonNull(collisionDetector, "collisionDetector");
        Objects.requireNonNull(segmentConsumer, "segmentConsumer");
        validateMaximumSteps(maximumSteps);

        TrajectoryVector position = start;
        TrajectoryVector velocity = initialVelocity;
        for (int step = 0; step < maximumSteps; step++) {
            if (physics.updateTiming() == UpdateTiming.BEFORE_MOVE) {
                velocity = physics.nextVelocity(velocity);
            }
            TrajectoryVector next = position.add(velocity);
            Optional<TrajectoryVector> collision = collisionDetector.firstCollision(position, next);
            TrajectoryVector endpoint = collision.orElse(next);
            segmentConsumer.segment(position, endpoint);
            if (collision.isPresent()) {
                return new TraceResult(endpoint, true);
            }
            position = next;
            if (physics.updateTiming() == UpdateTiming.AFTER_MOVE) {
                velocity = physics.nextVelocity(velocity);
            }
        }
        return new TraceResult(position, false);
    }

    public interface CollisionDetector {
        Optional<TrajectoryVector> firstCollision(TrajectoryVector from, TrajectoryVector to);
    }

    public interface SegmentConsumer {
        void segment(TrajectoryVector from, TrajectoryVector to);
    }

    /** Verified projectile-family gravity and drag rules, independent of Minecraft types. */
    public record Physics(double gravity, double drag, GravityOrder gravityOrder, UpdateTiming updateTiming) {
        public Physics {
            if (!Double.isFinite(gravity) || gravity < 0.0D || !Double.isFinite(drag) || drag <= 0.0D || drag > 1.0D
                    || gravityOrder == null || updateTiming == null) {
                throw new IllegalArgumentException("Invalid trajectory physics");
            }
        }

        public TrajectoryVector nextVelocity(TrajectoryVector velocity) {
            return gravityOrder == GravityOrder.AFTER_DRAG
                    ? velocity.scale(drag).subtractY(gravity)
                    : velocity.subtractY(gravity).scale(drag);
        }
    }

    public enum GravityOrder {
        AFTER_DRAG,
        BEFORE_DRAG
    }

    /** Whether Minecraft updates projectile velocity before or after movement in a tick. */
    public enum UpdateTiming {
        AFTER_MOVE,
        BEFORE_MOVE
    }

    public record Result(List<TrajectoryVector> points, boolean collided) {
        public Result {
            points = List.copyOf(points);
            if (points.isEmpty()) {
                throw new IllegalArgumentException("Trajectory result requires a start point");
            }
        }
    }

    public record TraceResult(TrajectoryVector terminalPoint, boolean collided) {
    }

    private static void validateMaximumSteps(int maximumSteps) {
        if (maximumSteps < 1 || maximumSteps > 400) {
            throw new IllegalArgumentException("maximumSteps must be between 1 and 400");
        }
    }
}
