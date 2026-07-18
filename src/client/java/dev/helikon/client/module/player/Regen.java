package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Requests a bounded burst of ordinary grounded movement updates while the
 * local player is hurt and fed. Modern vanilla servers normally regenerate by
 * server tick and may ignore the extra updates entirely.
 */
public final class Regen extends Module {
    public record Context(float health, float maximumHealth, int foodLevel, boolean screenOpen,
                          boolean onGround, boolean passenger, boolean flying, boolean fallFlying) {
        public Context {
            if (!Float.isFinite(health) || !Float.isFinite(maximumHealth)
                    || health < 0.0F || maximumHealth <= 0.0F || foodLevel < 0 || foodLevel > 20) {
                throw new IllegalArgumentException("regen context is invalid");
            }
        }
    }

    private final NumberSetting healthThreshold;
    private final IntegerSetting minimumFood;
    private final IntegerSetting packetsPerBurst;
    private final IntegerSetting delayTicks;
    private long lastBurstTick = -1L;

    public Regen() {
        super("regen", "Regen",
                "Requests bounded grounded movement bursts while hurt to accelerate healing on permissive servers.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Run at or below this observed health value.", 16.0D, 1.0D, 20.0D));
        minimumFood = addSetting(new IntegerSetting("minimum_food", "Minimum food",
                "Require at least this much observed food before requesting a burst.", 18, 0, 20));
        packetsPerBurst = addSetting(new IntegerSetting("packets_per_burst", "Packets per burst",
                "Bounded ordinary grounded movement updates sent per activation.", 5, 1, 10));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay",
                "Minimum client ticks between bursts.", 1, 1, 20));
    }

    /** Returns the bounded number of ordinary status updates to request this tick. */
    public int packetCount(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("regen tick and context are required");
        }
        if (!isEnabled() || context.screenOpen() || !context.onGround() || context.passenger()
                || context.flying() || context.fallFlying() || context.health() >= context.maximumHealth()
                || context.health() > healthThreshold.value() || context.foodLevel() < minimumFood.value()
                || (lastBurstTick >= 0L && tick - lastBurstTick < delayTicks.value())) {
            return 0;
        }
        lastBurstTick = tick;
        return packetsPerBurst.value();
    }

    public void onContextLost() {
        lastBurstTick = -1L;
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }
}
