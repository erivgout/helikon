package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpClickTest {
    private static TpClick enabled() {
        TpClick module = new TpClick();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    @Test
    void hasStableMovementIdentityAndSafeDefaults() {
        TpClick module = new TpClick();
        assertEquals("tp_click", module.id());
        assertEquals(ModuleCategory.MOVEMENT, module.category());
        assertFalse(module.defaultEnabled());
        assertFalse(module.keybind().isBound());
        assertTrue(module.consumesKeybindInput());
        assertTrue(module.cancelVelocity());
    }

    @Test
    void placesFeetAdjacentToTheLookedAtFace() {
        TpClick module = enabled();

        // Looking at the top face: stand on top of the block.
        assertEquals(new TpClick.Destination(10.5D, 65.0D, 20.5D),
                module.destination(10, 64, 20, 0, 1, 0, 3.0D).orElseThrow());

        // Looking at a side face: stand beside the block at its own level.
        assertEquals(new TpClick.Destination(12.5D, 64.0D, 20.5D),
                module.destination(11, 64, 20, 1, 0, 0, 3.0D).orElseThrow());

        // Looking at the bottom face: drop to the block below.
        assertEquals(new TpClick.Destination(10.5D, 63.0D, 20.5D),
                module.destination(10, 64, 20, 0, -1, 0, 3.0D).orElseThrow());
    }

    @Test
    void rejectsDisabledOutOfRangeAndInvalidInput() {
        TpClick disabled = new TpClick();
        assertFalse(disabled.destination(10, 64, 20, 0, 1, 0, 3.0D).isPresent());

        TpClick module = enabled();
        // Default max distance is 64; a farther target is rejected.
        assertFalse(module.destination(10, 64, 20, 0, 1, 0, 64.1D).isPresent());
        assertTrue(module.destination(10, 64, 20, 0, 1, 0, 64.0D).isPresent());
        // A zero/negative measured distance is not a valid target.
        assertFalse(module.destination(10, 64, 20, 0, 1, 0, 0.0D).isPresent());

        // A non-unit or non-finite face/distance is a programming error.
        assertThrows(IllegalArgumentException.class,
                () -> module.destination(10, 64, 20, 1, 1, 0, 3.0D));
        assertThrows(IllegalArgumentException.class,
                () -> module.destination(10, 64, 20, 0, 0, 0, 3.0D));
        assertThrows(IllegalArgumentException.class,
                () -> module.destination(10, 64, 20, 0, 1, 0, Double.NaN));
    }

    @Test
    void triggersOnlyOnTheRisingEdgeWhileEnabledAndNoScreen() {
        TpClick module = enabled();
        // onEnable requires a release first, so an initially-held key does not fire.
        assertFalse(module.pollTrigger(true, false));
        assertFalse(module.pollTrigger(false, false));
        // Fresh press fires exactly once; holding does not repeat.
        assertTrue(module.pollTrigger(true, false));
        assertFalse(module.pollTrigger(true, false));
        assertFalse(module.pollTrigger(false, false));
        // A press while a screen is open never fires.
        assertFalse(module.pollTrigger(true, true));
        // Physical state was tracked, so continuing to hold after the screen closes is not a fresh press.
        assertFalse(module.pollTrigger(true, false));
    }

    @Test
    void doesNotTriggerWhileDisabled() {
        TpClick module = new TpClick();
        assertFalse(module.pollTrigger(false, false));
        assertFalse(module.pollTrigger(true, false));

        Optional<TpClick.Destination> destination = module.destination(0, 0, 0, 0, 1, 0, 1.0D);
        assertFalse(destination.isPresent());
    }
}
