package dev.helikon.client.module.world;

import java.util.List;
import java.util.Objects;

/** Minecraft-free horizontal slide policy for one local player movement near cactus collision boxes. */
public final class CactusCollisionPolicy {
    private CactusCollisionPolicy() {
    }

    /** Returns a safe horizontal slide only when the full requested move would enter a cactus collision box. */
    public static Movement avoid(Movement requested, Bounds playerBounds, List<Bounds> cactusBounds) {
        Objects.requireNonNull(requested, "requested");
        Objects.requireNonNull(playerBounds, "playerBounds");
        Objects.requireNonNull(cactusBounds, "cactusBounds");
        if (requested.x() == 0.0D && requested.z() == 0.0D
                || !intersectsAny(playerBounds.move(requested), cactusBounds)) {
            return requested;
        }

        Movement xOnly = new Movement(requested.x(), requested.y(), 0.0D);
        Movement zOnly = new Movement(0.0D, requested.y(), requested.z());
        boolean xBlocked = intersectsAny(playerBounds.move(xOnly), cactusBounds);
        boolean zBlocked = intersectsAny(playerBounds.move(zOnly), cactusBounds);
        if (!xBlocked && zBlocked) {
            return xOnly;
        }
        if (xBlocked && !zBlocked) {
            return zOnly;
        }
        if (!xBlocked) {
            return Math.abs(requested.x()) >= Math.abs(requested.z()) ? xOnly : zOnly;
        }
        return new Movement(0.0D, requested.y(), 0.0D);
    }

    private static boolean intersectsAny(Bounds movedPlayer, List<Bounds> cactusBounds) {
        for (Bounds cactus : cactusBounds) {
            if (movedPlayer.intersects(Objects.requireNonNull(cactus, "cactus bounds"))) {
                return true;
            }
        }
        return false;
    }

    /** Finite local movement delta. */
    public record Movement(double x, double y, double z) {
        public Movement {
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
                throw new IllegalArgumentException("movement must be finite");
            }
        }
    }

    /** Finite non-empty axis-aligned box. */
    public record Bounds(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        public Bounds {
            if (!Double.isFinite(minX) || !Double.isFinite(minY) || !Double.isFinite(minZ)
                    || !Double.isFinite(maxX) || !Double.isFinite(maxY) || !Double.isFinite(maxZ)
                    || minX >= maxX || minY >= maxY || minZ >= maxZ) {
                throw new IllegalArgumentException("bounds must be finite and non-empty");
            }
        }

        public Bounds move(Movement movement) {
            return new Bounds(minX + movement.x(), minY + movement.y(), minZ + movement.z(),
                    maxX + movement.x(), maxY + movement.y(), maxZ + movement.z());
        }

        public boolean intersects(Bounds other) {
            return maxX > other.minX && minX < other.maxX
                    && maxY > other.minY && minY < other.maxY
                    && maxZ > other.minZ && minZ < other.maxZ;
        }
    }
}
