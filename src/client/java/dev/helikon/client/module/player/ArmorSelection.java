package dev.helikon.client.module.player;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Selects one safe, strictly better armor replacement without Minecraft UI dependencies. */
public final class ArmorSelection {
    private ArmorSelection() {
    }

    public record Upgrade(int sourceMenuSlot, ArmorSlot equipmentSlot, int destinationMenuSlot) {
        public Upgrade {
            if (sourceMenuSlot < 0 || destinationMenuSlot < 0) {
                throw new IllegalArgumentException("menu slots must be non-negative");
            }
            equipmentSlot = Objects.requireNonNull(equipmentSlot, "equipmentSlot");
        }
    }

    public static Optional<Upgrade> bestUpgrade(List<ArmorCandidate> candidates,
                                                 Map<ArmorSlot, ArmorCandidate> equipped,
                                                 Map<ArmorSlot, Integer> destinationSlots,
                                                 boolean preferDurability,
                                                 boolean protectBindingCurse,
                                                 double minimumImprovement) {
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(equipped, "equipped");
        Objects.requireNonNull(destinationSlots, "destinationSlots");
        if (!Double.isFinite(minimumImprovement) || minimumImprovement < 0.0D) {
            throw new IllegalArgumentException("minimumImprovement must be finite and non-negative");
        }
        Map<ArmorSlot, ArmorCandidate> current = new EnumMap<>(ArmorSlot.class);
        current.putAll(equipped);
        return candidates.stream()
                .filter(candidate -> destinationSlots.containsKey(candidate.equipmentSlot()))
                .filter(candidate -> isImprovement(candidate, current.get(candidate.equipmentSlot()),
                        preferDurability, protectBindingCurse, minimumImprovement))
                .max(Comparator.comparingDouble((ArmorCandidate candidate) ->
                                improvement(candidate, current.get(candidate.equipmentSlot()), preferDurability))
                        .thenComparing(ArmorCandidate::durabilityFraction)
                        .thenComparing(Comparator.comparingInt(ArmorCandidate::menuSlot).reversed()))
                .map(candidate -> new Upgrade(candidate.menuSlot(), candidate.equipmentSlot(),
                        destinationSlots.get(candidate.equipmentSlot())));
    }

    private static boolean isImprovement(ArmorCandidate candidate, ArmorCandidate current,
                                         boolean preferDurability, boolean protectBindingCurse,
                                         double minimumImprovement) {
        if (current != null && protectBindingCurse && current.bindingCurse()) {
            return false;
        }
        return improvement(candidate, current, preferDurability) > minimumImprovement;
    }

    private static double improvement(ArmorCandidate candidate, ArmorCandidate current, boolean preferDurability) {
        return candidate.score(preferDurability) - (current == null ? 0.0D : current.score(preferDurability));
    }
}
