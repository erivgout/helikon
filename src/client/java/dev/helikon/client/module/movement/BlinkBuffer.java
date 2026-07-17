package dev.helikon.client.module.movement;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A bounded, thread-safe first-in/first-out hold of pending items. Blink stores
 * outgoing movement packets here while enabled and drains them in send order
 * when it releases. The buffer is deliberately Minecraft-free so its ordering
 * and draining rules stay unit-testable; the packet type is supplied by the
 * caller through the generic parameter.
 */
public final class BlinkBuffer<T> {
    private final Deque<T> pending = new ArrayDeque<>();
    private final Object lock = new Object();

    /** Appends one item to the end of the hold. */
    public void add(T item) {
        Objects.requireNonNull(item, "item");
        synchronized (lock) {
            pending.addLast(item);
        }
    }

    public int size() {
        synchronized (lock) {
            return pending.size();
        }
    }

    public boolean isEmpty() {
        synchronized (lock) {
            return pending.isEmpty();
        }
    }

    /** Returns every held item in send order and empties the hold. */
    public List<T> drain() {
        synchronized (lock) {
            List<T> drained = List.copyOf(pending);
            pending.clear();
            return drained;
        }
    }

    /** Discards every held item without returning it. */
    public void clear() {
        synchronized (lock) {
            pending.clear();
        }
    }
}
