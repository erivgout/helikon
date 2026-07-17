package dev.helikon.client.render;

/** Immutable Minecraft-free three-dimensional value used by local trajectory prediction. */
public record TrajectoryVector(double x, double y, double z) {
    public TrajectoryVector {
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("Trajectory coordinates must be finite");
        }
    }

    public TrajectoryVector add(TrajectoryVector other) {
        return new TrajectoryVector(x + other.x, y + other.y, z + other.z);
    }

    public TrajectoryVector scale(double factor) {
        if (!Double.isFinite(factor)) {
            throw new IllegalArgumentException("Trajectory scale must be finite");
        }
        return new TrajectoryVector(x * factor, y * factor, z * factor);
    }

    public TrajectoryVector subtractY(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("Trajectory gravity must be finite");
        }
        return new TrajectoryVector(x, y - value, z);
    }
}
