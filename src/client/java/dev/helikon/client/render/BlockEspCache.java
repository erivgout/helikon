package dev.helikon.client.render;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;

/** Bounded insertion-ordered local block result cache updated by the incremental scanner. */
public final class BlockEspCache {
    private final int maximumEntries;
    private final Set<BlockEspScanCursor.Position> positions = new LinkedHashSet<>();
    private final Iterable<BlockEspScanCursor.Position> readOnlyPositions = Collections.unmodifiableSet(positions);

    public BlockEspCache(int maximumEntries) {
        if (maximumEntries < 1) {
            throw new IllegalArgumentException("maximumEntries must be positive");
        }
        this.maximumEntries = maximumEntries;
    }

    /** Updates one scanned coordinate, retaining only matching blocks and evicting the oldest entry if needed. */
    public void observe(BlockEspScanCursor.Position position, boolean matches) {
        if (!matches) {
            positions.remove(position);
            return;
        }
        positions.remove(position);
        positions.add(position);
        while (positions.size() > maximumEntries) {
            positions.remove(positions.iterator().next());
        }
    }

    public void clear() {
        positions.clear();
    }

    /** Revalidates retained entries without allocating a per-tick cache snapshot. */
    public void retain(Predicate<BlockEspScanCursor.Position> predicate) {
        positions.removeIf(position -> !predicate.test(position));
    }

    /** Read-only live iteration for a single-threaded render adapter; no per-frame snapshot is allocated. */
    public Iterable<BlockEspScanCursor.Position> positions() {
        return readOnlyPositions;
    }

    public int size() {
        return positions.size();
    }
}
