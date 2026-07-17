package dev.helikon.client.module.world;

/** Unit axis vector for BuilderAssist's Minecraft-free structure plans. */
public record BuildVector(int x, int y, int z) {
    public static final BuildVector UP = new BuildVector(0, 1, 0);

    public BuildVector {
        if (Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) {
            throw new IllegalArgumentException("BuildVector must be one axis-aligned unit vector");
        }
    }
}
