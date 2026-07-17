package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/** Releases a player-provided vanilla bow after a short configurable draw. */
public final class FastBow extends Module {
    private final IntegerSetting drawTicks;

    public FastBow() {
        super("fast_bow", "FastBow", "Releases a held vanilla bow after a short configured draw time.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        drawTicks = addSetting(new IntegerSetting("draw_ticks", "Draw ticks",
                "Ticks to draw before sending the ordinary release action.", 4, 1, 20));
    }

    public boolean shouldRelease(boolean bow, boolean usingItem, int usedTicks, boolean screenOpen) {
        return isEnabled() && bow && usingItem && !screenOpen && usedTicks >= drawTicks.value();
    }
}
