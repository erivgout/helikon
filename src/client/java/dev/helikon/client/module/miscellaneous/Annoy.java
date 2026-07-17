package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/** Produces a sparse ordinary main-hand swing decision from local client facts. */
public final class Annoy extends Module {
    /** Minecraft-free facts needed before an ordinary hand swing may be requested. */
    public record Context(boolean playerAvailable, boolean screenOpen) {
    }

    private final NumberSetting intervalTicks;
    private long nextSwingTick = Long.MIN_VALUE;

    public Annoy() {
        super("annoy", "Annoy", "Makes a sparse ordinary main-hand swing while enabled.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        intervalTicks = addSetting(new NumberSetting("interval_ticks", "Interval ticks",
                "Minimum local ticks between ordinary hand swings.", 40.0D, 20.0D, 600.0D));
    }

    /** Returns whether the narrow Minecraft adapter may make one ordinary hand swing now. */
    public boolean shouldSwing(long clientTick, Context context) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        Context current = Objects.requireNonNull(context, "context");
        if (!isEnabled() || !current.playerAvailable() || current.screenOpen() || clientTick < nextSwingTick) {
            return false;
        }

        long interval = intervalTicks();
        nextSwingTick = clientTick > Long.MAX_VALUE - interval ? Long.MAX_VALUE : clientTick + interval;
        return true;
    }

    @Override
    protected void onEnable() {
        nextSwingTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        nextSwingTick = Long.MIN_VALUE;
    }

    private long intervalTicks() {
        return Math.round(intervalTicks.value());
    }
}
