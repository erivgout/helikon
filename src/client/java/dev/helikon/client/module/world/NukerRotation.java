package dev.helikon.client.module.world;

/** Minecraft-free local view rotation toward one observed block center. */
public final class NukerRotation {
    public record Rotation(float yaw, float pitch) {
        public Rotation {
            if (!Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
                throw new IllegalArgumentException("rotation is invalid");
            }
        }
    }

    private NukerRotation() {
    }

    public static Rotation toward(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        if (!Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
            throw new IllegalArgumentException("eye position must be finite");
        }
        double x = blockX + 0.5D - eyeX;
        double y = blockY + 0.5D - eyeY;
        double z = blockZ + 0.5D - eyeZ;
        double horizontal = Math.sqrt(x * x + z * z);
        float yaw = wrap((float) Math.toDegrees(Math.atan2(-x, z)));
        float pitch = (float) -Math.toDegrees(Math.atan2(y, horizontal));
        return new Rotation(yaw, Math.max(-90.0F, Math.min(90.0F, pitch)));
    }

    private static float wrap(float value) {
        float wrapped = value % 360.0F;
        if (wrapped >= 180.0F) {
            return wrapped - 360.0F;
        }
        return wrapped < -180.0F ? wrapped + 360.0F : wrapped;
    }
}
