package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryFillPolicyTest {
    // Hotbar slots 0-8 map to menu slots 36-44; main inventory slots 9-35 map to menu slots 9-35.
    private static final Map<Integer, Integer> MENU_SLOTS = menuSlots();

    @Test
    void fillsAnEmptyTargetSlotFromReserves() {
        InventoryFillPolicy.Fill fill = InventoryFillPolicy.nextFill(
                List.of(item(18, 9, "minecraft:cobblestone", 64)),
                MENU_SLOTS, Map.of("minecraft:cobblestone", 0), true, true, 1).orElseThrow();

        assertEquals(18, fill.sourceMenuSlot());
        assertEquals(36, fill.targetMenuSlot());
    }

    @Test
    void refillsATargetHoldingLessThanTheMinimumCount() {
        var result = InventoryFillPolicy.nextFill(
                List.of(item(36, 0, "minecraft:arrow", 3), item(18, 9, "minecraft:arrow", 64)),
                MENU_SLOTS, Map.of("minecraft:arrow", 0), true, true, 16);

        assertTrue(result.isPresent());
        assertEquals(18, result.get().sourceMenuSlot());
        assertEquals(36, result.get().targetMenuSlot());
    }

    @Test
    void leavesATargetAtOrAboveTheMinimumUntouched() {
        assertFalse(InventoryFillPolicy.nextFill(
                List.of(item(36, 0, "minecraft:arrow", 32), item(18, 9, "minecraft:arrow", 64)),
                MENU_SLOTS, Map.of("minecraft:arrow", 0), true, true, 16).isPresent());
    }

    @Test
    void neverDisplacesADifferentItemInTheTargetSlot() {
        assertFalse(InventoryFillPolicy.nextFill(
                List.of(item(36, 0, "minecraft:diamond_sword", 1), item(18, 9, "minecraft:cobblestone", 64)),
                MENU_SLOTS, Map.of("minecraft:cobblestone", 0), true, true, 1).isPresent());
    }

    @Test
    void skipsProtectedNamedAndEnchantedReserves() {
        InventoryItem named = new InventoryItem(18, 9, "minecraft:cobblestone", 64, true, false, 0);
        InventoryItem enchanted = new InventoryItem(19, 10, "minecraft:cobblestone", 64, false, true, 0);
        assertFalse(InventoryFillPolicy.nextFill(List.of(named, enchanted),
                MENU_SLOTS, Map.of("minecraft:cobblestone", 0), true, true, 1).isPresent());

        var allowed = InventoryFillPolicy.nextFill(List.of(named, enchanted),
                MENU_SLOTS, Map.of("minecraft:cobblestone", 0), false, false, 1);
        assertTrue(allowed.isPresent());
        assertEquals(18, allowed.get().sourceMenuSlot());
    }

    @Test
    void neverPullsReservesOutOfAnotherManagedTargetSlot() {
        // Slot 1 (menu 37) holds cobblestone but is itself a managed target, so it is off-limits as a source.
        assertFalse(InventoryFillPolicy.nextFill(
                List.of(item(37, 1, "minecraft:cobblestone", 64)),
                MENU_SLOTS, Map.of("minecraft:cobblestone", 0, "minecraft:torch", 1), true, true, 1).isPresent());
    }

    @Test
    void moduleEmitsAThrottledReversibleSwapSequence() {
        InventoryFill module = new InventoryFill();
        module.enable();
        setFillTargets(module, "minecraft:cobblestone=0");

        List<InventoryItem> items = List.of(item(18, 9, "minecraft:cobblestone", 64));
        var first = module.nextAction(0L, items, MENU_SLOTS);
        assertTrue(first.isPresent());
        assertEquals(3, first.get().size());
        assertEquals(ContainerClick.Type.PICKUP, first.get().getFirst().type());
        assertEquals(18, first.get().getFirst().slot());
        assertEquals(36, first.get().get(1).slot());

        // Throttled: default delay is two ticks, so the very next tick yields nothing.
        assertFalse(module.nextAction(1L, items, MENU_SLOTS).isPresent());
    }

    private static void setFillTargets(InventoryFill module, String value) {
        module.settings().stream()
                .filter(setting -> setting.id().equals("fill_targets"))
                .map(setting -> (dev.helikon.client.setting.StringSetting) setting)
                .findFirst().orElseThrow()
                .set(value);
    }

    private static InventoryItem item(int menuSlot, int inventorySlot, String itemId, int count) {
        return new InventoryItem(menuSlot, inventorySlot, itemId, count, false, false, 0);
    }

    private static Map<Integer, Integer> menuSlots() {
        var slots = new java.util.LinkedHashMap<Integer, Integer>();
        for (int inventorySlot = 9; inventorySlot <= 35; inventorySlot++) {
            slots.put(inventorySlot, inventorySlot);
        }
        for (int hotbar = 0; hotbar <= 8; hotbar++) {
            slots.put(hotbar, 36 + hotbar);
        }
        return Map.copyOf(slots);
    }
}
