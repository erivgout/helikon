package dev.helikon.client.module.combat;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * A bounded first-in-first-out delay queue used by BackTrack to hold timestamped
 * payloads (held position packets) until a release deadline elapses.
 *
 * <p>This type is deliberately Minecraft-free so the delay/ordering decision can be
 * unit tested in isolation. Releases always happen in insertion order: the head gates
 * the queue, so held payloads for a single entity are never reordered even if the
 * configured delay changes between enqueues.
 */
public final class BackTrackBuffer<T> {
    private record Entry<T>(long releaseAtMillis, T payload) {
    }

    private final int maxEntries;
    private final Deque<Entry<T>> entries = new ArrayDeque<>();

    public BackTrackBuffer(int maxEntries) {
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1");
        }
        this.maxEntries = maxEntries;
    }

    /**
     * Holds {@code payload} until {@code releaseAtMillis}. Returns {@code false} without
     * storing anything when the buffer is already full, signalling the caller to let the
     * payload through immediately instead of growing memory without bound.
     */
    public boolean enqueue(T payload, long releaseAtMillis) {
        Objects.requireNonNull(payload, "payload");
        if (entries.size() >= maxEntries) {
            return false;
        }
        entries.addLast(new Entry<>(releaseAtMillis, payload));
        return true;
    }

    /** Removes and returns every payload whose deadline is at or before {@code nowMillis}, in insertion order. */
    public List<T> drainDue(long nowMillis) {
        List<T> due = new ArrayList<>();
        while (!entries.isEmpty() && entries.peekFirst().releaseAtMillis() <= nowMillis) {
            due.add(entries.pollFirst().payload());
        }
        return due;
    }

    /** Removes and returns every held payload in insertion order, regardless of deadline. */
    public List<T> drainAll() {
        List<T> all = new ArrayList<>(entries.size());
        while (!entries.isEmpty()) {
            all.add(entries.pollFirst().payload());
        }
        return all;
    }

    /** Discards every held payload without returning it. */
    public void clear() {
        entries.clear();
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int capacity() {
        return maxEntries;
    }
}
