package dev.helikon.client.gui;

/**
 * Minecraft-free persisted ClickGUI placement. An unset position centers the
 * window; saved coordinates are always clamped to the current GUI viewport.
 */
public final class ClickGuiWindowState {
    public static final int MAX_COORDINATE = 10_000;

    private boolean positioned;
    private int x;
    private int y;

    public boolean isPositioned() {
        return positioned;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    /** Sets a user-selected top-left location if both persisted values are safe. */
    public boolean setPosition(int x, int y) {
        if (!isValidCoordinate(x) || !isValidCoordinate(y)) {
            return false;
        }
        this.x = x;
        this.y = y;
        positioned = true;
        return true;
    }

    /** Clears a saved position so the next GUI open centers the window. */
    public void reset() {
        positioned = false;
        x = 0;
        y = 0;
    }

    /** Resolves a centered or saved placement while keeping the whole window visible. */
    public Position resolve(int viewportWidth, int viewportHeight, int windowWidth, int windowHeight) {
        validateDimensions(viewportWidth, viewportHeight, windowWidth, windowHeight);
        int maximumX = Math.max(0, viewportWidth - windowWidth);
        int maximumY = Math.max(0, viewportHeight - windowHeight);
        if (!positioned) {
            return new Position(maximumX / 2, maximumY / 2);
        }

        int resolvedX = Math.clamp(x, 0, maximumX);
        int resolvedY = Math.clamp(y, 0, maximumY);
        x = resolvedX;
        y = resolvedY;
        return new Position(resolvedX, resolvedY);
    }

    public static boolean isValidCoordinate(int coordinate) {
        return coordinate >= 0 && coordinate <= MAX_COORDINATE;
    }

    private static void validateDimensions(int viewportWidth, int viewportHeight, int windowWidth, int windowHeight) {
        if (viewportWidth < 0 || viewportHeight < 0 || windowWidth < 0 || windowHeight < 0) {
            throw new IllegalArgumentException("GUI dimensions cannot be negative");
        }
    }

    /** Resolved window coordinates in scaled GUI units. */
    public record Position(int x, int y) {
    }
}
