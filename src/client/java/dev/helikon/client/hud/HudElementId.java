package dev.helikon.client.hud;

/** Stable local IDs and safe initial anchors for registered HUD renderers. */
public enum HudElementId {
    WAYPOINTS(Anchor.TOP_LEFT, 5, 50),
    COORDINATES(Anchor.BOTTOM_LEFT, 5, 5),
    SATURATION(Anchor.BOTTOM_LEFT, 5, 5),
    ELYTRA(Anchor.TOP_LEFT, 5, 222),
    TARGET(Anchor.TOP_LEFT, 5, 260),
    REACH(Anchor.TOP_LEFT, 5, 324),
    INVENTORY_PREVIEW(Anchor.BOTTOM_RIGHT, 5, 5),
    DURABILITY_WARNINGS(Anchor.TOP_LEFT, 5, 112),
    RADAR(Anchor.TOP_LEFT, 5, 120),
    MINI_PLAYER(Anchor.TOP_LEFT, 5, 210),
    DEBUG_OVERLAY(Anchor.TOP_LEFT, 5, 5),
    BETTER_CROSSHAIR(Anchor.CENTER, 0, 0);

    private final Anchor defaultAnchor;
    private final int defaultOffsetX;
    private final int defaultOffsetY;

    HudElementId(Anchor defaultAnchor, int defaultOffsetX, int defaultOffsetY) {
        this.defaultAnchor = defaultAnchor;
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
    }

    public Anchor defaultAnchor() { return defaultAnchor; }
    public int defaultOffsetX() { return defaultOffsetX; }
    public int defaultOffsetY() { return defaultOffsetY; }

    public enum Anchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
}
