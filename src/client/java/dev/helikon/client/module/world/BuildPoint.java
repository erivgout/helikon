package dev.helikon.client.module.world;

/** Immutable Minecraft-free block coordinate for BuilderAssist planning. */
public record BuildPoint(int x, int y, int z) {
    public BuildPoint offset(BuildVector vector, int distance) {
        return new BuildPoint(Math.addExact(x, Math.multiplyExact(vector.x(), distance)),
                Math.addExact(y, Math.multiplyExact(vector.y(), distance)),
                Math.addExact(z, Math.multiplyExact(vector.z(), distance)));
    }
}
