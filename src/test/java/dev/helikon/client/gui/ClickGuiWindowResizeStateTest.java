package dev.helikon.client.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClickGuiWindowResizeStateTest {
    @Test
    void resizeKeepsTopLeftStableAndClampsToViewport() {
        ClickGuiWindowState window = new ClickGuiWindowState();
        window.setPosition(20, 30);
        ClickGuiWindowResizeState resize = new ClickGuiWindowResizeState(window);
        resize.beginResize(380, 250, new ClickGuiWindowState.Position(20, 30),
                new ClickGuiWindowState.Size(360, 220));

        assertTrue(resize.resizeTo(800, 700, new ClickGuiWindowState.Position(20, 30), 500, 400));
        assertEquals(480, window.width());
        assertEquals(370, window.height());

        resize.endResize();
        assertFalse(resize.isResizing());
        assertFalse(resize.resizeTo(300, 300, new ClickGuiWindowState.Position(20, 30), 500, 400));
    }

    @Test
    void resizeHonorsMinimumDimensions() {
        ClickGuiWindowState window = new ClickGuiWindowState();
        ClickGuiWindowResizeState resize = new ClickGuiWindowResizeState(window);
        resize.beginResize(360, 220, new ClickGuiWindowState.Position(0, 0),
                new ClickGuiWindowState.Size(360, 220));

        assertTrue(resize.resizeTo(10, 10, new ClickGuiWindowState.Position(0, 0), 500, 400));
        assertEquals(ClickGuiWindowState.MIN_WIDTH, window.width());
        assertEquals(ClickGuiWindowState.MIN_HEIGHT, window.height());
    }

    @Test
    void resizeWorksInViewportsSmallerThanTheNormalMinimum() {
        ClickGuiWindowState window = new ClickGuiWindowState();
        ClickGuiWindowResizeState resize = new ClickGuiWindowResizeState(window);
        resize.beginResize(180, 120, new ClickGuiWindowState.Position(0, 0),
                new ClickGuiWindowState.Size(180, 120));

        assertTrue(resize.resizeTo(100, 80, new ClickGuiWindowState.Position(0, 0), 180, 120));
        assertEquals(100, window.width());
        assertEquals(80, window.height());
        assertEquals(new ClickGuiWindowState.Size(100, 80), window.resolveSize(180, 120));
    }

    @Test
    void anchoredResizeKeepsTheExistingTopLeftPosition() {
        ClickGuiWindowState window = new ClickGuiWindowState();
        window.setPosition(70, 40);
        ClickGuiWindowResizeState resize = new ClickGuiWindowResizeState(window);
        resize.beginResize(430, 260, new ClickGuiWindowState.Position(70, 40),
                new ClickGuiWindowState.Size(360, 220));

        assertTrue(resize.resizeTo(480, 280, new ClickGuiWindowState.Position(70, 40), 600, 400));
        assertEquals(new ClickGuiWindowState.Position(70, 40),
                window.resolve(600, 400, window.width(), window.height()));
    }
}
