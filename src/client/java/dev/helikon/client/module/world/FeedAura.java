package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;

/** Selects one nearby loaded animal that accepts the currently held breeding food. */
public final class FeedAura extends Module {
    private final NumberSetting range;
    private final IntegerSetting delayTicks;
    private long lastFeedTick = Long.MIN_VALUE;

    public FeedAura() {
        super("feed_aura", "FeedAura", "Uses held breeding food on one nearby loaded animal.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Range", "Maximum animal interaction distance.",
                4.0D, 1.0D, 6.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay ticks",
                "Minimum ticks between ordinary feed interactions.", 10, 2, 100));
    }

    public OptionalInt select(long tick, boolean screenOpen, boolean holdingFood, List<Candidate> candidates) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("FeedAura inputs are invalid");
        }
        if (!isEnabled() || screenOpen || !holdingFood
                || (lastFeedTick != Long.MIN_VALUE && tick - lastFeedTick < delayTicks.value())) {
            return OptionalInt.empty();
        }
        return candidates.stream().filter(Candidate::acceptsFood)
                .filter(candidate -> candidate.distance() <= range.value())
                .min(Comparator.comparingDouble(Candidate::distance))
                .map(candidate -> OptionalInt.of(candidate.entityId()))
                .orElseGet(OptionalInt::empty);
    }

    public void markFed(long tick) {
        lastFeedTick = tick;
    }

    public void onContextLost() {
        lastFeedTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }

    public record Candidate(int entityId, double distance, boolean acceptsFood) {
    }
}
