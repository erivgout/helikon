package dev.helikon.client.hud;

import java.util.Objects;

/**
 * Minecraft-free drag state for the minimal HUD editor. It owns clamping and
 * pointer offsets so the screen only needs to provide input and dimensions.
 */
public final class HudEditorState {
    public static final int SNAP_DISTANCE = 8;
    private final HudLayout layout;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public HudEditorState(HudLayout layout) {
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Starts dragging only when the pointer is inside the enabled element. */
    public boolean beginDrag(int mouseX, int mouseY, HudBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (!layout.activeModulesEnabled() || !bounds.contains(mouseX, mouseY)) {
            return false;
        }

        dragging = true;
        dragOffsetX = mouseX - bounds.x();
        dragOffsetY = mouseY - bounds.y();
        return true;
    }

    /** Moves the dragged element while keeping its full footprint on screen. */
    public boolean dragTo(int mouseX, int mouseY, int viewportWidth, int viewportHeight, HudBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (!dragging) {
            return false;
        }
        if (viewportWidth < 0 || viewportHeight < 0) {
            throw new IllegalArgumentException("Viewport dimensions cannot be negative");
        }

        int maximumX = Math.max(0, viewportWidth - bounds.width());
        int maximumY = Math.max(0, viewportHeight - bounds.height());
        int x = snap(Math.clamp(mouseX - dragOffsetX, 0, maximumX), maximumX, viewportWidth, bounds.width());
        int y = snap(Math.clamp(mouseY - dragOffsetY, 0, maximumY), maximumY, viewportHeight, bounds.height());
        return layout.setActiveModulesPosition(x, y);
    }

    /** Clamps an existing persisted layout after a GUI-size change. */
    public void clampToViewport(int viewportWidth, int viewportHeight, HudBounds bounds) {
        Objects.requireNonNull(bounds, "bounds");
        if (viewportWidth < 0 || viewportHeight < 0) {
            throw new IllegalArgumentException("Viewport dimensions cannot be negative");
        }

        int maximumX = Math.max(0, viewportWidth - bounds.width());
        int maximumY = Math.max(0, viewportHeight - bounds.height());
        layout.setActiveModulesPosition(
                Math.clamp(bounds.x(), 0, maximumX),
                Math.clamp(bounds.y(), 0, maximumY)
        );
    }

    public void endDrag() {
        dragging = false;
    }

    private static int snap(int value, int maximum, int viewportSize, int elementSize) {
        if (value <= SNAP_DISTANCE) {
            return 0;
        }
        if (maximum - value <= SNAP_DISTANCE) {
            return maximum;
        }
        int centered = Math.max(0, (viewportSize - elementSize) / 2);
        return Math.abs(value - centered) <= SNAP_DISTANCE ? centered : value;
    }
}
