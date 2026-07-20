package dev.helikon.client.module.combat;

import java.util.List;

/** Immutable Minecraft-free block coordinate used by Domain Expansion planning and tests. */
public record DomainPosition(int x, int y, int z) {
    public DomainPosition offset(int dx, int dy, int dz) {
        return new DomainPosition(x + dx, y + dy, z + dz);
    }

    public List<DomainPosition> neighbors() {
        return List.of(
                offset(1, 0, 0),
                offset(-1, 0, 0),
                offset(0, 1, 0),
                offset(0, -1, 0),
                offset(0, 0, 1),
                offset(0, 0, -1)
        );
    }

    public long horizontalDistanceSquared(double otherX, double otherZ) {
        double dx = x + 0.5D - otherX;
        double dz = z + 0.5D - otherZ;
        return Math.round(dx * dx + dz * dz);
    }
}
