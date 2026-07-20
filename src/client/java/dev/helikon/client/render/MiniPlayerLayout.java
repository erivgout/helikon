package dev.helikon.client.render;

import dev.helikon.client.hud.HudBounds;
import dev.helikon.client.hud.HudElementPlacement;

import java.util.Objects;

/** Minecraft-free fixed HUD geometry for the initial local mini-player widget. */
public final class MiniPlayerLayout {
    public static final int X = 5;
    public static final int Y = 205;
    public static final int WIDTH = 70;
    public static final int HEIGHT = 80;

    private MiniPlayerLayout() {
    }

    public static HudBounds bounds() {
        return new HudBounds(X, Y, WIDTH, HEIGHT);
    }

    /** Resolves the absolute scaled content rectangle required by Minecraft's PIP entity renderer. */
    public static HudBounds contentBounds(HudElementPlacement placement, int viewportWidth, int viewportHeight) {
        Objects.requireNonNull(placement, "placement");
        int padding = placement.padding();
        HudBounds outer = placement.scaledBounds(viewportWidth, viewportHeight,
                WIDTH + padding * 2, HEIGHT + padding * 2);
        int scaledPadding = Math.round(padding * placement.scale());
        return new HudBounds(outer.x() + scaledPadding, outer.y() + scaledPadding,
                Math.round(WIDTH * placement.scale()), Math.round(HEIGHT * placement.scale()));
    }

    public static int entitySize(double scale) {
        if (!Double.isFinite(scale) || scale < 0.5D || scale > 2.0D) {
            throw new IllegalArgumentException("scale must be between 0.5 and 2.0");
        }
        return Math.round((float) (30.0D * scale));
    }
}
