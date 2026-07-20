package dev.helikon.client.hud;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/** Bounded one-second rolling click counts for the local Keystrokes HUD. */
public final class ClickRateTracker {
    private static final long WINDOW_NANOS = 1_000_000_000L;
    private static final int MAX_RETAINED_CLICKS = 128;

    public enum Button {
        LEFT,
        RIGHT
    }

    private final Deque<Long> leftClicks = new ArrayDeque<>();
    private final Deque<Long> rightClicks = new ArrayDeque<>();

    public synchronized void record(Button button, long nowNanos) {
        Deque<Long> clicks = clicks(button);
        prune(clicks, nowNanos);
        clicks.addLast(nowNanos);
        while (clicks.size() > MAX_RETAINED_CLICKS) {
            clicks.removeFirst();
        }
    }

    public synchronized int clicksPerSecond(Button button, long nowNanos) {
        Deque<Long> clicks = clicks(button);
        prune(clicks, nowNanos);
        return clicks.size();
    }

    public synchronized void clear() {
        leftClicks.clear();
        rightClicks.clear();
    }

    private Deque<Long> clicks(Button button) {
        return switch (Objects.requireNonNull(button, "button")) {
            case LEFT -> leftClicks;
            case RIGHT -> rightClicks;
        };
    }

    private static void prune(Deque<Long> clicks, long nowNanos) {
        long cutoff = nowNanos - WINDOW_NANOS;
        while (!clicks.isEmpty() && clicks.getFirst() <= cutoff) {
            clicks.removeFirst();
        }
    }

}
