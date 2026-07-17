package dev.helikon.client.module.player;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/** Selects the best safe hotbar tool without Minecraft dependencies. */
public final class ToolSelection {
    private ToolSelection() {
    }

    /**
     * Prefers tools that are correct for drops, then highest destroy speed.
     * Non-tools at vanilla speed are ignored so AutoTool never switches merely
     * to replace one bare-hand-equivalent item with another.
     */
    public static OptionalInt bestSlot(List<ToolCandidate> candidates, int minimumRemainingDurability) {
        if (minimumRemainingDurability < 0) {
            throw new IllegalArgumentException("minimumRemainingDurability must be non-negative");
        }
        return Objects.requireNonNull(candidates, "candidates").stream()
                .filter(candidate -> candidate.remainingDurability() >= minimumRemainingDurability)
                .filter(candidate -> candidate.correctForDrops() || candidate.destroySpeed() > 1.0)
                .max(Comparator.comparing(ToolCandidate::correctForDrops)
                        .thenComparing(ToolCandidate::destroySpeed)
                        .thenComparing(Comparator.comparing(ToolCandidate::slot).reversed()))
                .map(candidate -> OptionalInt.of(candidate.slot()))
                .orElseGet(OptionalInt::empty);
    }
}
