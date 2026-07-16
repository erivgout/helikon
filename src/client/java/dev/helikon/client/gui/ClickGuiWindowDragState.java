package dev.helikon.client.gui;

import java.util.Objects;

/** Minecraft-free pointer-offset handling for dragging the ClickGUI window. */
public final class ClickGuiWindowDragState {
    private final ClickGuiWindowState window;

    private boolean dragging;
    private int dragOffsetX;
    private int dragOffsetY;

    public ClickGuiWindowDragState(ClickGuiWindowState window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    public boolean isDragging() {
        return dragging;
    }

    /** Starts a drag using the pointer's offset inside the existing window. */
    public void beginDrag(int mouseX, int mouseY, ClickGuiWindowState.Position position) {
        Objects.requireNonNull(position, "position");
        dragging = true;
        dragOffsetX = mouseX - position.x();
        dragOffsetY = mouseY - position.y();
    }

    /** Updates the persisted position and clamps the complete window to its viewport. */
    public boolean dragTo(int mouseX, int mouseY, int viewportWidth, int viewportHeight, int windowWidth, int windowHeight) {
        if (!dragging) {
            return false;
        }
        if (viewportWidth < 0 || viewportHeight < 0 || windowWidth < 0 || windowHeight < 0) {
            throw new IllegalArgumentException("GUI dimensions cannot be negative");
        }

        int maximumX = Math.max(0, viewportWidth - windowWidth);
        int maximumY = Math.max(0, viewportHeight - windowHeight);
        return window.setPosition(
                Math.clamp(mouseX - dragOffsetX, 0, maximumX),
                Math.clamp(mouseY - dragOffsetY, 0, maximumY)
        );
    }

    public void endDrag() {
        dragging = false;
    }
}
