package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

/** Requests one ordinary vanilla respawn after the local player dies. */
public final class AutoRespawn extends Module {
    private final IntegerSetting delayTicks;
    private long deathStartedTick = -1L;
    private boolean respawnRequested;

    public AutoRespawn() {
        super("auto_respawn", "AutoRespawn", "Requests a normal vanilla respawn after local death.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay", "Ticks to wait after local death before requesting respawn.",
                0, 0, 100));
    }

    /**
     * Returns whether the adapter should make this death's single normal respawn request.
     * The decision accepts only Minecraft-free local death facts.
     */
    public boolean shouldRequestRespawn(long tick, boolean dead) {
        if (tick < 0L) {
            throw new IllegalArgumentException("tick must not be negative");
        }
        if (!isEnabled() || !dead) {
            resetDeathState();
            return false;
        }
        if (deathStartedTick < 0L) {
            deathStartedTick = tick;
        }
        if (respawnRequested || tick - deathStartedTick < delayTicks.value()) {
            return false;
        }
        respawnRequested = true;
        return true;
    }

    /** Clears a stale death observation when the local player or world is unavailable. */
    public void onContextLost() {
        resetDeathState();
    }

    @Override
    protected void onDisable() {
        resetDeathState();
    }

    private void resetDeathState() {
        deathStartedTick = -1L;
        respawnRequested = false;
    }
}
