package dev.helikon.client.hud;

/** A rectangular HUD element footprint in scaled GUI coordinates. */
public record HudBounds(int x, int y, int width, int height) {
    public HudBounds {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("HUD bounds cannot have a negative size");
        }
    }

    /** Whether a scaled-GUI coordinate lies inside this footprint. */
    public boolean contains(int mouseX, int mouseY) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
