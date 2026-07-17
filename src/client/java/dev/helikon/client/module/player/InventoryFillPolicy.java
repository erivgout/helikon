package dev.helikon.client.module.player;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Minecraft-free decision logic for refilling configured hotbar slots from matching
 * inventory reserves. It picks one deterministic fill move at a time and never displaces
 * an unrelated item, so every emitted move is an ordinary, reversible container swap.
 */
public final class InventoryFillPolicy {
    private InventoryFillPolicy() {
    }

    /** One reserve-to-target move expressed only as menu-slot indices. */
    public record Fill(int sourceMenuSlot, int targetMenuSlot) {
        public Fill {
            if (sourceMenuSlot < 0 || targetMenuSlot < 0) {
                throw new IllegalArgumentException("menu slots must be non-negative");
            }
            if (sourceMenuSlot == targetMenuSlot) {
                throw new IllegalArgumentException("source and target must differ");
            }
        }
    }

    /**
     * Returns the next fill move, if any. A target is refilled only while it is empty or
     * already holds its configured item below {@code minimumCount}; a target occupied by a
     * different item is left untouched. Reserves that sit in another managed target slot are
     * never used as a source, which prevents managed slots from cycling into one another.
     *
     * @param items                    non-empty ordinary player-inventory items
     * @param menuSlotsByInventorySlot maps every inventory slot (0-35) to its menu slot
     * @param fillTargets              item id to hotbar slot (0-8) targets to keep stocked
     * @param excludeNamed             do not draw custom-named items from reserves
     * @param excludeEnchanted         do not draw enchanted items from reserves
     * @param minimumCount             refill while the target holds fewer than this many
     */
    public static Optional<Fill> nextFill(List<InventoryItem> items,
                                          Map<Integer, Integer> menuSlotsByInventorySlot,
                                          Map<String, Integer> fillTargets,
                                          boolean excludeNamed,
                                          boolean excludeEnchanted,
                                          int minimumCount) {
        if (fillTargets.isEmpty() || minimumCount < 1) {
            return Optional.empty();
        }
        Set<Integer> managedHotbarSlots = Set.copyOf(fillTargets.values());
        // Iterate targets by hotbar slot for a stable, deterministic order.
        Map<Integer, String> targetsByHotbarSlot = new TreeMap<>();
        for (Map.Entry<String, Integer> entry : fillTargets.entrySet()) {
            targetsByHotbarSlot.put(entry.getValue(), entry.getKey());
        }

        for (Map.Entry<Integer, String> target : targetsByHotbarSlot.entrySet()) {
            int hotbarSlot = target.getKey();
            String itemId = target.getValue();
            Integer targetMenuSlot = menuSlotsByInventorySlot.get(hotbarSlot);
            if (targetMenuSlot == null) {
                continue;
            }
            Optional<InventoryItem> occupant = items.stream()
                    .filter(item -> item.inventorySlot() == hotbarSlot)
                    .findFirst();
            if (occupant.isPresent() && !occupant.get().itemId().equals(itemId)) {
                continue; // A different item occupies the slot; never displace it.
            }
            int currentCount = occupant.map(InventoryItem::count).orElse(0);
            if (currentCount >= minimumCount) {
                continue;
            }
            Optional<InventoryItem> source = items.stream()
                    .filter(item -> item.itemId().equals(itemId))
                    .filter(item -> item.inventorySlot() != hotbarSlot)
                    .filter(item -> !managedHotbarSlots.contains(item.inventorySlot()))
                    .filter(item -> !(excludeNamed && item.customNamed()))
                    .filter(item -> !(excludeEnchanted && item.enchanted()))
                    .min(Comparator.comparingInt(InventoryItem::inventorySlot));
            if (source.isEmpty()) {
                continue;
            }
            return Optional.of(new Fill(source.get().menuSlot(), targetMenuSlot));
        }
        return Optional.empty();
    }
}
