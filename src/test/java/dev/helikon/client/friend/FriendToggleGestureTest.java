package dev.helikon.client.friend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FriendToggleGestureTest {
    @Test
    void emitsOnceForEachMiddleClickOnAPlayer() {
        FriendToggleGesture gesture = new FriendToggleGesture();

        assertTrue(gesture.update(false, false, "Alice_1").isEmpty());
        assertEquals("Alice_1", gesture.update(true, false, "Alice_1").orElseThrow());
        assertTrue(gesture.update(true, false, "Alice_1").isEmpty());
        assertTrue(gesture.update(false, false, "Alice_1").isEmpty());
        assertEquals("Alice_1", gesture.update(true, false, "Alice_1").orElseThrow());
    }

    @Test
    void ignoresClicksOnScreensAndNonPlayersUntilRelease() {
        FriendToggleGesture gesture = new FriendToggleGesture();

        assertTrue(gesture.update(true, true, "Alice_1").isEmpty());
        assertTrue(gesture.update(true, false, "Alice_1").isEmpty());
        assertTrue(gesture.update(false, false, null).isEmpty());
        assertTrue(gesture.update(true, false, null).isEmpty());
        assertTrue(gesture.update(false, false, null).isEmpty());
        assertEquals("Alice_1", gesture.update(true, false, "Alice_1").orElseThrow());
    }
}
