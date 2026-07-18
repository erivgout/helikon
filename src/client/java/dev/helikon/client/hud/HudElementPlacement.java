package dev.helikon.client.hud;

import java.util.Objects;

/** Persisted enabled state and anchored local placement for one HUD element. */
public final class HudElementPlacement {
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 2.0F;
    public static final int MIN_PADDING = 0;
    public static final int MAX_PADDING = 12;
    public static final int DEFAULT_COLOR = 0xFFE5EDF5;

    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }

    private boolean enabled;
    private final HudElementId element;
    private HudElementId.Anchor anchor;
    private int offsetX;
    private int offsetY;
    private float scale;
    private Alignment alignment;
    private boolean background;
    private int padding;
    private boolean textShadow;
    private int color;
    private boolean rainbow;

    public HudElementPlacement(HudElementId element) {
        this.element = Objects.requireNonNull(element, "element");
        reset(this.element);
    }

    public boolean enabled() { return enabled; }
    public HudElementId.Anchor anchor() { return anchor; }
    public int offsetX() { return offsetX; }
    public int offsetY() { return offsetY; }
    public float scale() { return scale; }
    public Alignment alignment() { return alignment; }
    public boolean background() { return background; }
    public int padding() { return padding; }
    public boolean textShadow() { return textShadow; }
    public int color() { return color; }
    public boolean rainbow() { return rainbow; }
    public boolean positionLocked() { return element.positionLocked(); }

    public void setEnabled(boolean value) { enabled = value; }

    public boolean setScale(float value) {
        if (!Float.isFinite(value) || value < MIN_SCALE || value > MAX_SCALE) {
            return false;
        }
        scale = value;
        return true;
    }

    public void setAlignment(Alignment value) { alignment = Objects.requireNonNull(value, "value"); }

    public void setBackground(boolean value) { background = value; }

    public boolean setPadding(int value) {
        if (value < MIN_PADDING || value > MAX_PADDING) {
            return false;
        }
        padding = value;
        return true;
    }

    public void setTextShadow(boolean value) { textShadow = value; }

    public void setColor(int value) { color = value; }

    public void setRainbow(boolean value) { rainbow = value; }

    public boolean set(HudElementId.Anchor nextAnchor, int nextOffsetX, int nextOffsetY) {
        if (!HudLayout.isValidCoordinate(nextOffsetX) || !HudLayout.isValidCoordinate(nextOffsetY)) {
            return false;
        }
        if (positionLocked()) {
            anchor = element.defaultAnchor();
            offsetX = element.defaultOffsetX();
            offsetY = element.defaultOffsetY();
            return true;
        }
        anchor = Objects.requireNonNull(nextAnchor, "nextAnchor");
        offsetX = nextOffsetX;
        offsetY = nextOffsetY;
        return true;
    }

    /** Converts a dragged screen position to a stable top-left placement. */
    public boolean setAbsolutePosition(int x, int y) {
        if (positionLocked()) {
            return false;
        }
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

    /** Resolves a placement for content that is drawn at this element's selected scale. */
    public HudBounds scaledBounds(int viewportWidth, int viewportHeight, int contentWidth, int contentHeight) {
        if (contentWidth < 0 || contentHeight < 0) {
            throw new IllegalArgumentException("HUD dimensions cannot be negative");
        }
        return bounds(viewportWidth, viewportHeight, Math.max(0, Math.round(contentWidth * scale)),
                Math.max(0, Math.round(contentHeight * scale)));
    }

    public void reset(HudElementId element) {
        Objects.requireNonNull(element, "element");
        enabled = element.defaultEnabled();
        anchor = element.defaultAnchor();
        offsetX = element.defaultOffsetX();
        offsetY = element.defaultOffsetY();
        scale = 1.0F;
        alignment = Alignment.LEFT;
        background = true;
        padding = 3;
        textShadow = true;
        color = DEFAULT_COLOR;
        rainbow = false;
    }
}
