package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/**
 * Holds outgoing player movement packets locally while enabled and releases
 * them in order later. The server keeps receiving no position updates while
 * packets are held, so it treats the player as standing still; releasing the
 * hold sends the queued moves and the server catches up (it may also reject,
 * correct, rubber-band, or kick). The pure hold/release decision lives here so
 * it stays unit-testable; a narrow connection adapter performs the actual
 * buffering and re-send.
 */
public final class Blink extends Module {
    private final IntegerSetting maximumHeldPackets;

    public Blink() {
        super("blink", "Blink", "Holds outgoing movement packets and releases them later.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        maximumHeldPackets = addSetting(new IntegerSetting("max_held_packets", "Max held packets",
                "Safety cap on buffered movement packets; the hold is released automatically when it is reached.",
                20, 1, 200));
    }

    public int maximumHeldPackets() {
        return maximumHeldPackets.value();
    }

    /**
     * Whether one more outgoing movement packet should be held rather than sent.
     * Held only while enabled and below the configured safety cap.
     */
    public boolean shouldHold(int currentlyHeld) {
        if (currentlyHeld < 0) {
            throw new IllegalArgumentException("currentlyHeld must be non-negative");
        }
        return isEnabled() && currentlyHeld < maximumHeldPackets();
    }

    /**
     * Whether the current hold should be released. Always releases what is held
     * once the module is disabled, and releases early when the safety cap is
     * reached so buffered desync stays bounded.
     */
    public boolean shouldRelease(int currentlyHeld) {
        if (currentlyHeld < 0) {
            throw new IllegalArgumentException("currentlyHeld must be non-negative");
        }
        if (currentlyHeld == 0) {
            return false;
        }
        return !isEnabled() || currentlyHeld >= maximumHeldPackets();
    }
}
