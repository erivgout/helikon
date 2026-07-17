package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

/**
 * Withholds the local player's outgoing movement packets and releases them in a delayed
 * burst, creating deliberate client-side position lag ("blink"/FakeLag). The server stays
 * authoritative: it may reject, correct, rubber-band, or kick, so this promises only a
 * local timing effect, never a guaranteed server outcome.
 *
 * <p>The module owns only its configuration; the buffering and re-send bridge lives in
 * {@link FakeLagAccess}, which restores the withheld packets whenever the module is
 * disabled, the world is left, or panic runs.
 */
public final class FakeLag extends Module {
    private final NumberSetting delayMillis;
    private final NumberSetting maxHeldPackets;

    public FakeLag() {
        super("fakelag", "FakeLag",
                "Delays outgoing movement packets and releases them in a burst; servers remain authoritative.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        delayMillis = addSetting(new NumberSetting("delay_ms", "Delay (ms)",
                "How long each held movement packet is withheld before it is released.",
                200.0D, 50.0D, 1000.0D));
        maxHeldPackets = addSetting(new NumberSetting("max_held", "Max held packets",
                "Upper bound on withheld packets; the oldest are released early past this cap.",
                20.0D, 1.0D, 100.0D));
    }

    public long delayMillis() {
        return Math.round(delayMillis.value());
    }

    public int maxHeldPackets() {
        return (int) Math.round(maxHeldPackets.value());
    }

    @Override
    protected void onDisable() {
        // Release everything Helikon withheld so the local player's reported position
        // reconverges with the server instead of leaving packets stranded.
        FakeLagAccess.flushAll();
    }
}
