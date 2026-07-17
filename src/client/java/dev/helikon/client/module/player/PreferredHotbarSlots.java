package dev.helikon.client.module.player;

import java.util.LinkedHashMap;
import java.util.Map;

/** Parses exact item-ID to hotbar-slot preferences without Minecraft dependencies. */
public final class PreferredHotbarSlots {
    private PreferredHotbarSlots() {
    }

    public static Map<String, Integer> parse(String text) {
        Map<String, Integer> result = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String rawEntry : text.split(",")) {
            String entry = rawEntry.trim();
            int separator = entry.lastIndexOf('=');
            if (separator < 1 || separator != entry.indexOf('=')) {
                throw new IllegalArgumentException("Expected item_id=hotbar_slot: " + entry);
            }
            String itemId = entry.substring(0, separator).trim();
            if (!InventorySlotRules.itemIds(itemId).contains(itemId)) {
                throw new IllegalArgumentException("Invalid item identifier: " + itemId);
            }
            int slot;
            try {
                slot = Integer.parseInt(entry.substring(separator + 1).trim());
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid hotbar slot: " + entry, exception);
            }
            if (slot < 0 || slot > 8) {
                throw new IllegalArgumentException("Hotbar slot must be between 0 and 8: " + slot);
            }
            if (result.putIfAbsent(itemId, slot) != null) {
                throw new IllegalArgumentException("Duplicate preferred item: " + itemId);
            }
        }
        return Map.copyOf(result);
    }
}
