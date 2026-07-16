package dev.helikon.client.friend;

import java.util.Optional;

/**
 * Detects a safe middle-click edge for the thin Minecraft friend-toggle
 * adapter. It deliberately knows nothing about entity or input APIs.
 */
public final class FriendToggleGesture {
    private boolean wasMiddlePressed;

    /**
     * Returns the targeted player name once for a new middle-click. A click
     * begun while a screen is open is ignored until the button is released.
     */
    public Optional<String> update(boolean middlePressed, boolean screenOpen, String targetedPlayerName) {
        if (!middlePressed) {
            wasMiddlePressed = false;
            return Optional.empty();
        }
        if (wasMiddlePressed) {
            return Optional.empty();
        }

        wasMiddlePressed = true;
        if (screenOpen || targetedPlayerName == null) {
            return Optional.empty();
        }
        return Optional.of(targetedPlayerName);
    }
}
