package dev.helikon.client.render;

/**
 * Minecraft-free camera-space direction and off-screen decision for on-screen enemy arrows.
 *
 * <p>Given a world-space delta from the camera to a target plus the camera yaw/pitch, this
 * projects the target onto the camera's right/up basis and reports whether it lies outside a
 * simple angular field-of-view cone. The returned direction is a normalized screen-space vector
 * (x grows right, y grows down) an adapter can use to place an arrow around the screen center.
 */
public final class ArrowProjection {
    private ArrowProjection() {
    }

    public static Result project(double deltaX, double deltaY, double deltaZ,
                                 double yawDegrees, double pitchDegrees, double fieldOfViewDegrees) {
        if (!Double.isFinite(deltaX) || !Double.isFinite(deltaY) || !Double.isFinite(deltaZ)
                || !Double.isFinite(yawDegrees) || !Double.isFinite(pitchDegrees)
                || !Double.isFinite(fieldOfViewDegrees) || fieldOfViewDegrees <= 0.0D
                || fieldOfViewDegrees >= 360.0D) {
            throw new IllegalArgumentException("Invalid arrow projection input");
        }
        double length = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
        if (length < 1.0e-6D) {
            return new Result(false, 0.0D, 1.0D, 0.0D);
        }

        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        double sinYaw = Math.sin(yaw);
        double cosYaw = Math.cos(yaw);
        double sinPitch = Math.sin(pitch);
        double cosPitch = Math.cos(pitch);

        // Minecraft camera basis in world space.
        double forwardX = -sinYaw * cosPitch;
        double forwardY = -sinPitch;
        double forwardZ = cosYaw * cosPitch;
        double rightX = cosYaw;
        double rightZ = sinYaw;
        double upX = -sinYaw * sinPitch;
        double upY = cosPitch;
        double upZ = cosYaw * sinPitch;

        double forward = (deltaX * forwardX + deltaY * forwardY + deltaZ * forwardZ) / length;
        double right = deltaX * rightX + deltaZ * rightZ;
        double up = deltaX * upX + deltaY * upY + deltaZ * upZ;

        double angleFromForward = Math.toDegrees(Math.acos(Math.max(-1.0D, Math.min(1.0D, forward))));
        boolean outside = angleFromForward > fieldOfViewDegrees / 2.0D;

        // Screen space: x grows right, y grows down. The world "right" basis vector points to the
        // player's left, so negate it to place the arrow on the correct side of the screen. Up is
        // negated because screen y increases downward.
        double screenX = -right;
        double screenY = -up;
        double planar = Math.sqrt(screenX * screenX + screenY * screenY);
        if (planar < 1.0e-9D) {
            // Target lies almost exactly ahead or behind: point straight down.
            return new Result(outside, 0.0D, 1.0D, length);
        }
        return new Result(outside, screenX / planar, screenY / planar, length);
    }

    /** Immutable off-screen decision and normalized screen-space arrow direction. */
    public record Result(boolean outside, double directionX, double directionY, double distance) {
    }
}
