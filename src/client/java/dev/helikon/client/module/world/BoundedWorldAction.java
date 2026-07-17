package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Shared Minecraft-free cadence and nearest-candidate policy for small bounded world actions. */
public abstract class BoundedWorldAction extends Module {
    private final NumberSetting range;
    private final IntegerSetting delayTicks;
    private long lastActionTick = Long.MIN_VALUE;

    protected BoundedWorldAction(String id, String name, String description, double defaultRange, int defaultDelay) {
        super(id, name, description, ModuleCategory.WORLD, false, Keybind.unbound());
        range = addSetting(new NumberSetting("range", "Range", "Maximum loaded block distance.",
                defaultRange, 1.0D, 6.0D));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay ticks",
                "Minimum ticks between ordinary interaction attempts.", defaultDelay, 1, 100));
    }

    public Optional<Candidate> select(long tick, boolean screenOpen, List<Candidate> candidates) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("World action inputs are invalid");
        }
        if (!isEnabled() || screenOpen
                || (lastActionTick != Long.MIN_VALUE && tick - lastActionTick < delayTicks.value())) {
            return Optional.empty();
        }
        return candidates.stream()
                .filter(candidate -> candidate.distance() <= range.value())
                .filter(this::accepts)
                .min(Comparator.<Candidate>comparingDouble(candidate -> priority(candidate, tick))
                        .thenComparingInt(Candidate::y)
                        .thenComparingInt(Candidate::x)
                        .thenComparingInt(Candidate::z));
    }

    protected abstract boolean accepts(Candidate candidate);

    protected double priority(Candidate candidate, long tick) {
        return candidate.distance();
    }

    public void markActed(long tick) {
        lastActionTick = tick;
    }

    public int scanRadius() {
        return (int) Math.ceil(range.value());
    }

    public void onContextLost() {
        lastActionTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }

    public record Candidate(int x, int y, int z, String blockId, double distance,
                            boolean matureCrop, boolean growable, boolean tillable,
                            boolean liquidSource, boolean tnt, boolean log, boolean ore,
                            boolean excavatable, boolean tunnel, boolean replaceable) {
        public Candidate {
            if (blockId == null || blockId.isBlank() || !Double.isFinite(distance) || distance < 0.0D) {
                throw new IllegalArgumentException("World candidate is invalid");
            }
        }
    }
}
