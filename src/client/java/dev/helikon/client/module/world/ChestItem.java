package dev.helikon.client.module.world;

import java.util.Objects;

/** Minecraft-free fact record for a visible chest-menu item. */
public record ChestItem(int menuSlot, String itemId, int count, int priority) {
    public ChestItem {
        if (menuSlot < 0 || count < 1 || priority < 0) {
            throw new IllegalArgumentException("chest item facts are invalid");
        }
        itemId = Objects.requireNonNull(itemId, "itemId");
        if (!itemId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("itemId is invalid");
        }
    }
}
