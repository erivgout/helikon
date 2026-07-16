package dev.helikon.client.gui;

import java.util.Objects;

/** Minecraft-free bottom-right resize handling for the ClickGUI window. */
public final class ClickGuiWindowResizeState {
    private final ClickGuiWindowState window;
    private boolean resizing;
    private int widthOffset;
    private int heightOffset;

    public ClickGuiWindowResizeState(ClickGuiWindowState window) {
        this.window = Objects.requireNonNull(window, "window");
    }

    public boolean isResizing() {
        return resizing;
    }

    public void beginResize(int mouseX, int mouseY, ClickGuiWindowState.Position position, ClickGuiWindowState.Size size) {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(size, "size");
        resizing = true;
        widthOffset = size.width() - (mouseX - position.x());
        heightOffset = size.height() - (mouseY - position.y());
    }

    /** Keeps the top-left corner stable and bounds the resized window to the viewport. */
    public boolean resizeTo(int mouseX, int mouseY, ClickGuiWindowState.Position position, int viewportWidth, int viewportHeight) {
        Objects.requireNonNull(position, "position");
        if (!resizing) {
            return false;
        }
        int maximumWidth = Math.max(1, viewportWidth - position.x());
        int maximumHeight = Math.max(1, viewportHeight - position.y());
        int minimumWidth = maximumWidth < ClickGuiWindowState.MIN_WIDTH ? 1 : ClickGuiWindowState.MIN_WIDTH;
        int minimumHeight = maximumHeight < ClickGuiWindowState.MIN_HEIGHT ? 1 : ClickGuiWindowState.MIN_HEIGHT;
        int width = Math.clamp(mouseX - position.x() + widthOffset, minimumWidth, maximumWidth);
        int height = Math.clamp(mouseY - position.y() + heightOffset, minimumHeight, maximumHeight);
        return window.setSize(width, height);
    }

    public void endResize() {
        resizing = false;
    }
}
