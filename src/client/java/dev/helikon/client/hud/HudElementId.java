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
    BETTER_CROSSHAIR(Anchor.CENTER, 0, 0, true, true),
    HEALTH(Anchor.CENTER, 0, 14),
    DIRECTION(Anchor.TOP_RIGHT, 5, 5, false),
    FPS(Anchor.TOP_RIGHT, 5, 18, false),
    PING(Anchor.TOP_RIGHT, 5, 31, false),
    TPS(Anchor.TOP_RIGHT, 5, 44, false),
    SPEED(Anchor.TOP_RIGHT, 5, 57, false),
    ARMOR_DURABILITY(Anchor.TOP_RIGHT, 5, 70, false),
    HELD_ITEM_DURABILITY(Anchor.TOP_RIGHT, 5, 83, false),
    POTION_EFFECTS(Anchor.TOP_RIGHT, 5, 96, false),
    CLOCK(Anchor.TOP_RIGHT, 5, 109, false),
    BIOME(Anchor.TOP_RIGHT, 5, 122, false),
    SERVER_ADDRESS(Anchor.TOP_RIGHT, 5, 135, false),
    TOTEM_COUNT(Anchor.TOP_RIGHT, 5, 148, false);

    private final Anchor defaultAnchor;
    private final int defaultOffsetX;
    private final int defaultOffsetY;
    private final boolean defaultEnabled;
    private final boolean positionLocked;

    HudElementId(Anchor defaultAnchor, int defaultOffsetX, int defaultOffsetY) {
        this(defaultAnchor, defaultOffsetX, defaultOffsetY, true, false);
    }

    HudElementId(Anchor defaultAnchor, int defaultOffsetX, int defaultOffsetY, boolean defaultEnabled) {
        this(defaultAnchor, defaultOffsetX, defaultOffsetY, defaultEnabled, false);
    }

    HudElementId(Anchor defaultAnchor, int defaultOffsetX, int defaultOffsetY, boolean defaultEnabled,
                 boolean positionLocked) {
        this.defaultAnchor = defaultAnchor;
        this.defaultOffsetX = defaultOffsetX;
        this.defaultOffsetY = defaultOffsetY;
        this.defaultEnabled = defaultEnabled;
        this.positionLocked = positionLocked;
    }

    public Anchor defaultAnchor() { return defaultAnchor; }
    public int defaultOffsetX() { return defaultOffsetX; }
    public int defaultOffsetY() { return defaultOffsetY; }
    public boolean defaultEnabled() { return defaultEnabled; }
    public boolean positionLocked() { return positionLocked; }

    public enum Anchor {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
        CENTER
    }
}
