package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Reels a locally observed bobber bite, then recasts through Minecraft's normal Use interaction. */
public final class AutoFish extends Module {
    public enum Action {
        NONE,
        USE_HELD_ROD
    }

    private final NumberSetting reelDelayTicks;
    private final NumberSetting recastDelayTicks;
    private final NumberSetting minimumDurability;
    private final BooleanSetting openWaterOnly;
    private long nextActionTick;
    private long biteObservedTick = -1L;
    private boolean castIssued;
    private boolean awaitingRecast;

    public AutoFish() {
        super("auto_fish", "AutoFish", "Reels observed bobber bites and recasts through Minecraft's normal rod use.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        reelDelayTicks = addSetting(new NumberSetting("reel_delay_ticks", "Reel delay",
                "Minimum local ticks before reeling a newly observed bite.", 4.0D, 0.0D, 40.0D));
        recastDelayTicks = addSetting(new NumberSetting("recast_delay_ticks", "Recast delay",
                "Minimum local ticks after a reel before casting again.", 10.0D, 1.0D, 100.0D));
        minimumDurability = addSetting(new NumberSetting("minimum_durability", "Minimum durability",
                "Stop automation before a damageable rod reaches this remaining durability.", 8.0D, 0.0D, 2_032.0D));
        openWaterOnly = addSetting(new BooleanSetting("open_water_only", "Open water only",
                "Reel only when Minecraft identifies the current hook as open-water fishing.", true));
    }

    /** Returns at most one normal held-rod use action for this client tick. */
    public Action update(long tick, boolean heldRod, int remainingDurability, boolean hookPresent,
                         boolean hookBiting, boolean openWaterFishing) {
        if (remainingDurability < 0) {
            throw new IllegalArgumentException("remainingDurability must be non-negative");
        }
        if (!isEnabled() || !heldRod || remainingDurability < Math.round(minimumDurability.value())) {
            clearState();
            return Action.NONE;
        }
        if (hookPresent) {
            castIssued = false;
            if (awaitingRecast) {
                return Action.NONE;
            }
            if (!hookBiting || (openWaterOnly.value() && !openWaterFishing)) {
                biteObservedTick = -1L;
                return Action.NONE;
            }
            if (biteObservedTick < 0L) {
                biteObservedTick = tick;
            }
            if (tick < biteObservedTick + Math.round(reelDelayTicks.value())) {
                return Action.NONE;
            }
            awaitingRecast = true;
            biteObservedTick = -1L;
            nextActionTick = tick + Math.round(recastDelayTicks.value());
            return Action.USE_HELD_ROD;
        }
        biteObservedTick = -1L;
        if (awaitingRecast) {
            if (tick < nextActionTick) {
                return Action.NONE;
            }
            awaitingRecast = false;
            castIssued = true;
            return Action.USE_HELD_ROD;
        }
        if (!castIssued) {
            castIssued = true;
            return Action.USE_HELD_ROD;
        }
        return Action.NONE;
    }

    @Override
    protected void onDisable() {
        clearState();
    }

    private void clearState() {
        biteObservedTick = -1L;
        castIssued = false;
        awaitingRecast = false;
        nextActionTick = 0L;
    }
}
