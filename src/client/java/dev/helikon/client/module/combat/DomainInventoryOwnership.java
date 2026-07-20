package dev.helikon.client.module.combat;

/**
 * Minecraft-free selected-slot ownership. External slot changes suspend the owner instead of
 * causing two modules to repeatedly fight over the selected slot.
 */
public final class DomainInventoryOwnership {
    private int originalSlot = -1;
    private int ownedSlot = -1;

    public void acquire(int currentSlot, int blockSlot) {
        requireSlot(currentSlot);
        requireSlot(blockSlot);
        if (originalSlot < 0) {
            originalSlot = currentSlot;
        }
        ownedSlot = blockSlot;
    }

    public boolean hasConflict(int currentSlot) {
        requireSlot(currentSlot);
        return ownedSlot >= 0 && currentSlot != ownedSlot;
    }

    public int restorationSlot(int currentSlot) {
        requireSlot(currentSlot);
        int restoration = ownedSlot >= 0 && currentSlot == ownedSlot ? originalSlot : -1;
        clear();
        return restoration;
    }

    public int originalSlot() {
        return originalSlot;
    }

    public int ownedSlot() {
        return ownedSlot;
    }

    public void clear() {
        originalSlot = -1;
        ownedSlot = -1;
    }

    private static void requireSlot(int slot) {
        if (slot < 0 || slot > 8) {
            throw new IllegalArgumentException("slot must be a hotbar index");
        }
    }
}
