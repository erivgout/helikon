package dev.helikon.client.module.player;

import java.util.Objects;

/** One ordinary player-inventory item, represented without Minecraft types. */
public record InventoryItem(int menuSlot, int inventorySlot, String itemId, int count,
                            boolean customNamed, boolean enchanted, int remainingDurability) {
    public InventoryItem {
        if (menuSlot < 0 || inventorySlot < 0 || inventorySlot > 35) {
            throw new IllegalArgumentException("slot is outside the normal player inventory");
        }
        itemId = Objects.requireNonNull(itemId, "itemId");
        if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || count < 1 || remainingDurability < 0) {
            throw new IllegalArgumentException("inventory item facts are invalid");
        }
    }
}
