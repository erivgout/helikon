package dev.helikon.client.module.movement;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Minecraft-free FIFO buffer that holds items with an enqueue timestamp and releases
 * them once they have aged past a configured delay. Keeping this decision logic free of
 * game types lets it be unit tested deterministically; the thin adapter supplies the
 * clock and the real packet payloads.
 *
 * <p>The buffer is bounded per enqueue: passing a capacity force-releases the oldest
 * entries so memory stays bounded and the held burst cannot grow without limit. All
 * releasing methods preserve strict first-in, first-out order so the outgoing packet
 * stream is never reordered.
 *
 * <p>This class is not thread-safe; callers that touch it from more than one thread must
 * provide external synchronization.
 */
final class HeldPacketQueue<T> {
    private final Deque<Entry<T>> entries = new ArrayDeque<>();

    /**
     * Records an item as held at {@code nowMillis}. If the buffer exceeds {@code maxHeld}
     * the oldest entries are removed and returned so the caller can release them
     * immediately (FIFO). The returned list is normally empty.
     */
    List<T> enqueue(long nowMillis, T item, int maxHeld) {
        Objects.requireNonNull(item, "item");
        if (maxHeld < 1) {
            throw new IllegalArgumentException("maxHeld must be at least 1");
        }
        entries.addLast(new Entry<>(nowMillis, item));
        List<T> overflow = new ArrayList<>();
        while (entries.size() > maxHeld) {
            overflow.add(entries.removeFirst().item());
        }
        return overflow;
    }

    /**
     * Removes and returns, in FIFO order, every entry whose age at {@code nowMillis} is at
     * least {@code delayMillis}. Because entries are enqueued in non-decreasing time order,
     * releasing stops at the first entry that is still too young.
     */
    List<T> drainReleasable(long nowMillis, long delayMillis) {
        if (delayMillis < 0L) {
            throw new IllegalArgumentException("delayMillis must not be negative");
        }
        List<T> released = new ArrayList<>();
        while (!entries.isEmpty()) {
            Entry<T> head = entries.peekFirst();
            if (nowMillis - head.enqueuedAtMillis() < delayMillis) {
                break;
            }
            released.add(entries.removeFirst().item());
        }
        return released;
    }

    /** Removes and returns every held entry in FIFO order, emptying the buffer. */
    List<T> drainAll() {
        List<T> released = new ArrayList<>(entries.size());
        while (!entries.isEmpty()) {
            released.add(entries.removeFirst().item());
        }
        return released;
    }

    /** Discards every held entry without returning them. */
    void clear() {
        entries.clear();
    }

    int size() {
        return entries.size();
    }

    boolean isEmpty() {
        return entries.isEmpty();
    }

    private record Entry<T>(long enqueuedAtMillis, T item) {
        private Entry {
            Objects.requireNonNull(item, "item");
        }
    }
}
