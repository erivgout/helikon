package dev.helikon.client.module.combat;

/** Rectangular enclosure bounds. Walls sit directly outside the inclusive interior bounds. */
public record DomainBounds(
        int interiorMinX,
        int interiorMaxX,
        int interiorMinZ,
        int interiorMaxZ,
        int floorY,
        int interiorHeight
) {
    public DomainBounds {
        if (interiorMinX > interiorMaxX || interiorMinZ > interiorMaxZ || interiorHeight < 2) {
            throw new IllegalArgumentException("Domain bounds are invalid");
        }
    }

    public int wallMinX() {
        return interiorMinX - 1;
    }

    public int wallMaxX() {
        return interiorMaxX + 1;
    }

    public int wallMinZ() {
        return interiorMinZ - 1;
    }

    public int wallMaxZ() {
        return interiorMaxZ + 1;
    }

    public int roofY() {
        return floorY + interiorHeight;
    }

    public int floorBlockY() {
        return floorY - 1;
    }

    public int width() {
        return wallMaxX() - wallMinX() + 1;
    }

    public int length() {
        return wallMaxZ() - wallMinZ() + 1;
    }

    public boolean containsFeet(DomainPosition feet) {
        return feet.x() >= interiorMinX && feet.x() <= interiorMaxX
                && feet.z() >= interiorMinZ && feet.z() <= interiorMaxZ
                && feet.y() >= floorY && feet.y() + 1 < roofY();
    }

    public boolean containsHorizontal(double x, double z) {
        return x >= interiorMinX && x < interiorMaxX + 1.0D
                && z >= interiorMinZ && z < interiorMaxZ + 1.0D;
    }
}
