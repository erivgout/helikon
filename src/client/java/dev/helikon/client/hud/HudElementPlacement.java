package dev.helikon.client.hud;

import java.util.Objects;

/** Persisted enabled state and anchored local placement for one HUD element. */
public final class HudElementPlacement {
    private boolean enabled;
    private HudElementId.Anchor anchor;
    private int offsetX;
    private int offsetY;

    public HudElementPlacement(HudElementId element) {
        Objects.requireNonNull(element, "element");
        enabled = element.defaultEnabled();
        anchor = element.defaultAnchor();
        offsetX = element.defaultOffsetX();
        offsetY = element.defaultOffsetY();
    }

    public boolean enabled() { return enabled; }
    public HudElementId.Anchor anchor() { return anchor; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }

    public void setEnabled(boolean value) { enabled = value; }

    public boolean set(HudElementId.Anchor nextAnchor, int nextOffsetX, int nextOffsetY) {
        if (!HudLayout.isValidCoordinate(nextOffsetX) || !HudLayout.isValidCoordinate(nextOffsetY)) {
            return false;
        }
        anchor = Objects.requireNonNull(nextAnchor, "nextAnchor");
        offsetX = nextOffsetX;
        offsetY = nextOffsetY;
        return true;
    }

    /** Converts a dragged screen position to a stable top-left placement. */
    public boolean setAbsolutePosition(int x, int y) {
        return set(HudElementId.Anchor.TOP_LEFT, x, y);
    }

    public HudBounds bounds(int viewportWidth, int viewportHeight, int contentWidth, int contentHeight) {
        if (viewportWidth < 0 || viewportHeight < 0 || contentWidth < 0 || contentHeight < 0) {
            throw new IllegalArgumentException("HUD dimensions cannot be negative");
        }
        int x = switch (anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> Math.max(0, viewportWidth - contentWidth - offsetX);
            case CENTER -> Math.max(0, (viewportWidth - contentWidth) / 2 + offsetX);
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_RIGHT -> offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> Math.max(0, viewportHeight - contentHeight - offsetY);
            case CENTER -> Math.max(0, (viewportHeight - contentHeight) / 2 + offsetY);
        };
        return new HudBounds(Math.clamp(x, 0, Math.max(0, viewportWidth - contentWidth)),
                Math.clamp(y, 0, Math.max(0, viewportHeight - contentHeight)), contentWidth, contentHeight);
    }

    public void reset(HudElementId element) {
        Objects.requireNonNull(element, "element");
        enabled = element.defaultEnabled();
        anchor = element.defaultAnchor();
        offsetX = element.defaultOffsetX();
        offsetY = element.defaultOffsetY();
    }
}
