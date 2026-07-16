package dev.helikon.client.input;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PanicKeybindManagerTest {
    private static final int KEY = 82;

    @Test
    void triggersOncePerPressAndOnlyInPermittedScreenContexts() {
        PanicKeybindManager manager = new PanicKeybindManager();
        manager.setKeybind(new Keybind(KEY, Keybind.Activation.TOGGLE));
        Set<Integer> down = new HashSet<>();
        AtomicInteger triggered = new AtomicInteger();

        down.add(KEY);
        manager.tick(down::contains, false, false, triggered::incrementAndGet);
        manager.tick(down::contains, false, false, triggered::incrementAndGet);
        assertEquals(1, triggered.get());

        down.remove(KEY);
        manager.tick(down::contains, false, false, triggered::incrementAndGet);
        down.add(KEY);
        manager.tick(down::contains, true, false, triggered::incrementAndGet);
        assertEquals(1, triggered.get(), "typing in chat or another screen must suppress panic");

        down.remove(KEY);
        manager.tick(down::contains, true, false, triggered::incrementAndGet);
        down.add(KEY);
        manager.tick(down::contains, true, true, triggered::incrementAndGet);
        assertEquals(2, triggered.get(), "Helikon screens permit panic so it can close them");
    }

    @Test
    void escapePressCapturedFromAClosingOrdinaryScreenRemainsSuppressed() {
        PanicKeybindManager manager = new PanicKeybindManager();
        manager.setKeybind(new Keybind(GLFW.GLFW_KEY_ESCAPE, Keybind.Activation.TOGGLE));
        Set<Integer> down = Set.of(GLFW.GLFW_KEY_ESCAPE);
        AtomicInteger triggered = new AtomicInteger();

        manager.tick(down::contains, true, false, triggered::incrementAndGet);

        assertEquals(0, triggered.get(), "the start-of-tick ordinary-screen context suppresses Escape after vanilla closes it");
    }
}
