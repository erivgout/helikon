package dev.helikon.client.module.player;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Deterministic hotbar-food filtering and priority scoring. */
public final class FoodSelection {
    private FoodSelection() {
    }

    public static OptionalInt bestSlot(List<FoodCandidate> candidates, Set<String> avoidedItemIds,
                                       AutoEat.FoodPriority priority) {
        if (candidates == null || avoidedItemIds == null || priority == null) {
            throw new IllegalArgumentException("food-selection arguments must not be null");
        }
        Set<String> normalizedAvoided = avoidedItemIds.stream()
                .filter(itemId -> itemId != null)
                .map(itemId -> itemId.trim().toLowerCase(Locale.ROOT))
                .filter(itemId -> !itemId.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return candidates.stream()
                .filter(candidate -> !normalizedAvoided.contains(candidate.itemId()))
                .min(comparator(priority))
                .map(candidate -> OptionalInt.of(candidate.slot()))
                .orElseGet(OptionalInt::empty);
    }

    private static Comparator<FoodCandidate> comparator(AutoEat.FoodPriority priority) {
        return switch (priority) {
            case HOTBAR_ORDER -> Comparator.comparingInt(FoodCandidate::slot);
            case HIGHEST_NUTRITION -> Comparator.comparingInt(FoodCandidate::nutrition).reversed()
                    .thenComparing(Comparator.comparingDouble(FoodCandidate::saturationGain).reversed())
                    .thenComparingInt(FoodCandidate::slot);
            case HIGHEST_SATURATION -> Comparator.comparingDouble(FoodCandidate::saturationGain).reversed()
                    .thenComparing(Comparator.comparingInt(FoodCandidate::nutrition).reversed())
                    .thenComparingInt(FoodCandidate::slot);
        };
    }
}
