package dev.helikon.client.gui;

/**
 * Suppresses the repeat key and character events emitted for a key that was
 * just captured as a module bind. Suppression ends only on that key's release.
 */
public final class KeyCaptureSuppression {
    private static final int NO_SUPPRESSED_KEY = Integer.MIN_VALUE;

    private int suppressedKey = NO_SUPPRESSED_KEY;

    /** Starts consuming repeat input for the captured key. */
    public void begin(int keyCode) {
        suppressedKey = keyCode;
    }

    /** Whether a repeated key event should be consumed. */
    public boolean consumesKeyPress(int keyCode) {
        return suppressedKey == keyCode;
    }

    /**
     * Character events do not identify their source key, so all character
     * input remains consumed while the captured key is physically held.
     */
    public boolean consumesCharacterInput() {
        return suppressedKey != NO_SUPPRESSED_KEY;
    }

    /** Stops suppression when the originally captured key is released. */
    public boolean release(int keyCode) {
        if (suppressedKey != keyCode) {
            return false;
        }
        suppressedKey = NO_SUPPRESSED_KEY;
        return true;
    }

    public void clear() {
        suppressedKey = NO_SUPPRESSED_KEY;
    }
}
