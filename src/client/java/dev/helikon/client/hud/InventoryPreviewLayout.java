package dev.helikon.client.hud;

import java.util.ArrayList;
import java.util.List;

/** Minecraft-free selection and geometry rules for the local inventory preview grid. */
public final class InventoryPreviewLayout {
    public static final int COLUMNS = 9;
    public static final int HOTBAR_SIZE = 9;
    public static final int STORAGE_SIZE = 27;
    public static final int SLOT_SIZE = 18;
    public static final int PADDING = 3;

    private InventoryPreviewLayout() {
    }

    /** Selects verified non-equipment inventory indexes: storage first, then optional hotbar. */
    public static List<Integer> slots(int availableSlots, int storageRows, boolean includeHotbar) {
        if (availableSlots < 0 || storageRows < 1 || storageRows > STORAGE_SIZE / COLUMNS) {
            throw new IllegalArgumentException("Invalid inventory preview dimensions");
        }
        List<Integer> slots = new ArrayList<>(storageRows * COLUMNS + (includeHotbar ? HOTBAR_SIZE : 0));
        int storageEnd = Math.min(availableSlots, HOTBAR_SIZE + storageRows * COLUMNS);
        for (int slot = HOTBAR_SIZE; slot < storageEnd; slot++) {
            slots.add(slot);
        }
        if (includeHotbar) {
            int hotbarEnd = Math.min(availableSlots, HOTBAR_SIZE);
            for (int slot = 0; slot < hotbarEnd; slot++) {
                slots.add(slot);
            }
        }
        return List.copyOf(slots);
    }

    public static int rowsFor(int selectedSlots) {
        if (selectedSlots < 0) {
            throw new IllegalArgumentException("selectedSlots must not be negative");
        }
        return (selectedSlots + COLUMNS - 1) / COLUMNS;
    }

    public static int width() {
        return COLUMNS * SLOT_SIZE + PADDING * 2;
    }

    public static int height(int rows) {
        if (rows < 0) {
            throw new IllegalArgumentException("rows must not be negative");
        }
        return rows * SLOT_SIZE + PADDING * 2;
    }
}
