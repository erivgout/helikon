package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Bounded, insertion-ordered, context-aware chunk capture queue. */
public final class MapCaptureQueue {
    public static final int MAXIMUM_PENDING_CHUNKS = 512;

    private final Set<Entry> pending = new LinkedHashSet<>();

    public boolean offer(WaypointContext context, int chunkX, int chunkZ) {
        Entry entry = new Entry(context, chunkX, chunkZ);
        if (pending.contains(entry)) {
            return false;
        }
        if (pending.size() >= MAXIMUM_PENDING_CHUNKS) {
            return false;
        }
        return pending.add(entry);
    }

    /**
     * Moves a chunk to the front of the capture queue. If the queue is full,
     * the newest tail entry is discarded to make room; a dropped chunk that is
     * still loaded is re-offered by the next discovery reseed sweep.
     */
    public void prioritize(WaypointContext context, int chunkX, int chunkZ) {
        Entry entry = new Entry(context, chunkX, chunkZ);
        Set<Entry> reordered = new LinkedHashSet<>();
        reordered.add(entry);
        for (Entry existing : pending) {
            if (reordered.size() >= MAXIMUM_PENDING_CHUNKS) {
                break;
            }
            reordered.add(existing);
        }
        pending.clear();
        pending.addAll(reordered);
    }

    public Optional<Entry> peek() {
        Iterator<Entry> iterator = pending.iterator();
        return iterator.hasNext() ? Optional.of(iterator.next()) : Optional.empty();
    }

    public boolean remove(Entry entry) {
        return pending.remove(Objects.requireNonNull(entry, "entry"));
    }

    public void clear() {
        pending.clear();
    }

    public int size() {
        return pending.size();
    }

    public record Entry(WaypointContext context, int chunkX, int chunkZ) {
        public Entry {
            context = Objects.requireNonNull(context, "context");
        }
    }
}
