package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/** Pauses an integrated single-player server after beneficial-effect idle time. */
public final class PotionSaver extends Module {
    private final IntegerSetting idleTicks;

    public PotionSaver() {
        super("potion_saver", "PotionSaver",
                "Pauses single-player after standing still with a beneficial effect to preserve duration.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        idleTicks = addSetting(new IntegerSetting("idle_ticks", "Idle ticks",
                "Stationary ticks before pausing the integrated server.", 100, 20, 1200));
    }

    public boolean shouldPause(boolean singlePlayer, boolean beneficialEffect, boolean moving,
                               boolean screenOpen, int stationaryTicks) {
        return isEnabled() && singlePlayer && beneficialEffect && !moving && !screenOpen
                && stationaryTicks >= idleTicks.value();
    }
}
