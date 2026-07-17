package dev.helikon.client.module.player;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Parses bounded local inventory-slot and item-ID lists without Minecraft dependencies. */
public final class InventorySlotRules {
    private InventorySlotRules() {
    }

    public static Set<Integer> slots(String text, int maximumSlot) {
        if (maximumSlot < 0) {
            throw new IllegalArgumentException("maximumSlot must be non-negative");
        }
        Set<Integer> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String rawToken : text.split(",")) {
            String token = rawToken.trim();
            if (token.isEmpty()) {
                continue;
            }
            int separator = token.indexOf('-');
            if (separator < 0) {
                addSlot(result, parseSlot(token), maximumSlot);
                continue;
            }
            if (separator == 0 || separator != token.lastIndexOf('-')) {
                throw new IllegalArgumentException("Invalid slot range: " + token);
            }
            int from = parseSlot(token.substring(0, separator));
            int to = parseSlot(token.substring(separator + 1));
            if (from > to) {
                throw new IllegalArgumentException("Slot range must be ascending: " + token);
            }
            for (int slot = from; slot <= to; slot++) {
                addSlot(result, slot, maximumSlot);
            }
        }
        return Set.copyOf(result);
    }

    public static Set<String> itemIds(String text) {
        Set<String> result = new LinkedHashSet<>();
        if (text == null || text.isBlank()) {
            return result;
        }
        for (String rawToken : text.split(",")) {
            String token = rawToken.trim().toLowerCase(Locale.ROOT);
            if (token.isEmpty()) {
                continue;
            }
            if (!token.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException("Invalid item identifier: " + rawToken.trim());
            }
            result.add(token);
        }
        return Set.copyOf(result);
    }

    private static int parseSlot(String token) {
        try {
            return Integer.parseInt(token.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid inventory slot: " + token, exception);
        }
    }

    private static void addSlot(Set<Integer> result, int slot, int maximumSlot) {
        if (slot < 0 || slot > maximumSlot) {
            throw new IllegalArgumentException("Inventory slot is outside 0-" + maximumSlot + ": " + slot);
        }
        result.add(slot);
    }
}
