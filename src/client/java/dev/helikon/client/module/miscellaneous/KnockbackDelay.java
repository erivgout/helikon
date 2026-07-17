package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Bounded, Minecraft-free scheduler for delaying ordinary local-player entity-motion updates.
 * The narrow packet adapter decides which received packet belongs to the local player.
 */
public final class KnockbackDelay extends Module {
    static final int MAXIMUM_PENDING_MOTIONS = 32;

    @FunctionalInterface
    public interface MotionSink {
        void apply(Motion motion);
    }

    public record Motion(double x, double y, double z) {
        public boolean isFinite() {
            return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z);
        }
    }

    private record ScheduledMotion(long releaseTick, Motion motion) {
    }

    private final IntegerSetting delayTicks;
    private final MotionSink motionSink;
    private final ArrayDeque<ScheduledMotion> pending = new ArrayDeque<>();

    public KnockbackDelay(MotionSink motionSink) {
        super("knockback_delay", "KnockbackDelay",
                "Delays ordinary local-player entity-motion packets by a bounded number of client ticks.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        this.motionSink = Objects.requireNonNull(motionSink, "motionSink");
        delayTicks = addSetting(new IntegerSetting(
                "delay_ticks",
                "Delay ticks",
                "Client ticks to wait before applying a received local-player entity-motion packet.",
                5,
                1,
                40
        ));
    }

    /**
     * Queues one finite motion while enabled. A full queue declines interception so Minecraft can
     * apply the newest packet immediately instead of losing server-supplied motion.
     */
    public boolean delay(long receivedTick, Motion motion) {
        if (receivedTick < 0L) {
            throw new IllegalArgumentException("receivedTick must not be negative");
        }
        Motion received = Objects.requireNonNull(motion, "motion");
        if (!isEnabled() || !received.isFinite() || pending.size() >= MAXIMUM_PENDING_MOTIONS) {
            return false;
        }

        long delay = delayTicks.value();
        long releaseTick = receivedTick > Long.MAX_VALUE - delay ? Long.MAX_VALUE : receivedTick + delay;
        pending.addLast(new ScheduledMotion(releaseTick, received));
        return true;
    }

    /** Removes and returns every queued motion whose configured delay has elapsed, in arrival order. */
    public List<Motion> releaseReady(long clientTick) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        List<Motion> ready = new ArrayList<>();
        while (!pending.isEmpty() && pending.peekFirst().releaseTick() <= clientTick) {
            ready.add(pending.removeFirst().motion());
        }
        return List.copyOf(ready);
    }

    public int pendingCount() {
        return pending.size();
    }

    /** World-exit cleanup deliberately discards motion for an entity that no longer exists. */
    public void clearPending() {
        pending.clear();
    }

    @Override
    protected void onDisable() {
        while (!pending.isEmpty()) {
            motionSink.apply(pending.removeFirst().motion());
        }
    }
}
