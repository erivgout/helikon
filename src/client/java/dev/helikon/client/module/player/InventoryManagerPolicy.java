package dev.helikon.client.module.player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Deterministic, Minecraft-free policy for one conservative inventory-management action. */
public final class InventoryManagerPolicy {
    private InventoryManagerPolicy() {
    }

    public enum ActionType {
        SWAP,
        DROP_STACK
    }

    public record Action(ActionType type, int sourceMenuSlot, int destinationMenuSlot) {
        public Action {
            type = Objects.requireNonNull(type, "type");
            if (sourceMenuSlot < 0 || (type == ActionType.SWAP && destinationMenuSlot < 0)) {
                throw new IllegalArgumentException("menu slots are invalid");
            }
        }
    }

    public record Options(boolean sortInventory, boolean dropJunk, Set<String> junkItemIds,
                          Map<String, Integer> preferredHotbarSlots, boolean preserveNamed,
                          boolean preserveEnchanted, int minimumRemainingDurability) {
        public Options {
            junkItemIds = Set.copyOf(Objects.requireNonNull(junkItemIds, "junkItemIds"));
            preferredHotbarSlots = Map.copyOf(Objects.requireNonNull(preferredHotbarSlots, "preferredHotbarSlots"));
            if (minimumRemainingDurability < 0) {
                throw new IllegalArgumentException("minimumRemainingDurability must be non-negative");
            }
        }
    }

    public static Optional<Action> nextAction(List<InventoryItem> items, Map<Integer, Integer> menuSlotsByInventorySlot,
                                               Options options) {
        List<InventoryItem> current = List.copyOf(Objects.requireNonNull(items, "items"));
        Map<Integer, Integer> menuSlots = Map.copyOf(Objects.requireNonNull(menuSlotsByInventorySlot,
                "menuSlotsByInventorySlot"));
        Options config = Objects.requireNonNull(options, "options");
        if (config.dropJunk()) {
            Optional<InventoryItem> junk = current.stream()
                    .filter(item -> config.junkItemIds().contains(item.itemId()))
                    .filter(item -> isMovable(item, config))
                    .min(Comparator.comparingInt(InventoryItem::inventorySlot));
            if (junk.isPresent()) {
                return Optional.of(new Action(ActionType.DROP_STACK, junk.get().menuSlot(), -1));
            }
        }
        Optional<Action> preferred = preferredHotbarAction(current, menuSlots, config);
        if (preferred.isPresent()) {
            return preferred;
        }
        return config.sortInventory() ? sortAction(current, config) : Optional.empty();
    }

    private static Optional<Action> preferredHotbarAction(List<InventoryItem> items, Map<Integer, Integer> menuSlots,
                                                            Options options) {
        for (Map.Entry<String, Integer> preference : options.preferredHotbarSlots().entrySet()) {
            int targetInventorySlot = preference.getValue();
            if (items.stream().anyMatch(item -> item.inventorySlot() == targetInventorySlot)) {
                continue;
            }
            Optional<InventoryItem> source = items.stream()
                    .filter(item -> item.itemId().equals(preference.getKey()))
                    .filter(item -> isMovable(item, options))
                    .min(Comparator.comparingInt(InventoryItem::inventorySlot));
            Integer targetMenuSlot = menuSlots.get(targetInventorySlot);
            if (source.isPresent() && targetMenuSlot != null) {
                return Optional.of(new Action(ActionType.SWAP, source.get().menuSlot(), targetMenuSlot));
            }
        }
        return Optional.empty();
    }

    private static Optional<Action> sortAction(List<InventoryItem> items, Options options) {
        List<InventoryItem> movable = new ArrayList<>(items.stream().filter(item -> isMovable(item, options))
                .sorted(Comparator.comparingInt(InventoryItem::inventorySlot)).toList());
        for (int index = 0; index + 1 < movable.size(); index++) {
            InventoryItem left = movable.get(index);
            InventoryItem right = movable.get(index + 1);
            if (left.itemId().compareTo(right.itemId()) > 0) {
                return Optional.of(new Action(ActionType.SWAP, left.menuSlot(), right.menuSlot()));
            }
        }
        return Optional.empty();
    }

    private static boolean isMovable(InventoryItem item, Options options) {
        if (options.preserveNamed() && item.customNamed()) {
            return false;
        }
        if (options.preserveEnchanted() && item.enchanted()) {
            return false;
        }
        return item.remainingDurability() == 0 || item.remainingDurability() >= options.minimumRemainingDurability();
    }
}
