package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import net.minecraft.world.entity.EquipmentSlot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryAutomationPolicyTest {
    @Test
    void armorSelectsTheLargestStrictUpgrade() {
        AutoArmor module = new AutoArmor();
        module.enable();
        var result = module.nextAction(0L,
                List.of(new ArmorCandidate(12, ArmorSlot.HEAD, 3.0D, 0.0D, 1.0D, false),
                        new ArmorCandidate(13, ArmorSlot.CHEST, 8.0D, 2.0D, 1.0D, false)),
                Map.of(ArmorSlot.HEAD, new ArmorCandidate(5, ArmorSlot.HEAD, 1.0D, 0.0D, 1.0D, false),
                        ArmorSlot.CHEST, new ArmorCandidate(7, ArmorSlot.CHEST, 7.0D, 2.0D, 1.0D, false)),
                Map.of(ArmorSlot.HEAD, 5, ArmorSlot.CHEST, 7));

        assertTrue(result.isPresent());
        assertEquals(new ContainerClick(12, 0, ContainerClick.Type.PICKUP), result.get().getFirst());
    }

    @Test
    void armorNeverReplacesBindingCurseWhenProtected() {
        var result = ArmorSelection.bestUpgrade(
                List.of(new ArmorCandidate(12, ArmorSlot.HEAD, 4.0D, 0.0D, 1.0D, false)),
                Map.of(ArmorSlot.HEAD, new ArmorCandidate(5, ArmorSlot.HEAD, 1.0D, 0.0D, 1.0D, true)),
                Map.of(ArmorSlot.HEAD, 5), true, true, 0.0D);

        assertTrue(result.isEmpty());
    }

    @Test
    void slotAndIdentifierParsersRejectUnsafeText() {
        assertEquals(Set.of(0, 1, 2, 4), InventorySlotRules.slots("0-2,4", 8));
        assertEquals(Set.of("minecraft:stone"), InventorySlotRules.itemIds("minecraft:stone"));
        assertThrows(IllegalArgumentException.class, () -> InventorySlotRules.slots("3-1", 8));
        assertThrows(IllegalArgumentException.class, () -> InventorySlotRules.itemIds("../outside"));
        assertEquals(Map.of("minecraft:torch", 8), PreferredHotbarSlots.parse("minecraft:torch=8"));
    }

    @Test
    void autoEjectRespectsProtectedHotbarSlots() {
        AutoEject module = new AutoEject();
        module.enable();
        var result = module.nextAction(0L, List.of(
                item(9, 0, "minecraft:rotten_flesh"), item(18, 9, "minecraft:rotten_flesh")));

        assertTrue(result.isPresent());
        assertEquals(18, result.get().getFirst().slot());
        assertEquals(ContainerClick.Type.THROW, result.get().getFirst().type());
    }

    @Test
    void totemRestoresOnlyTheRecordedPriorItem() {
        AutoTotem module = new AutoTotem();
        module.enable();
        var equip = module.nextAction(0L, new AutoTotem.Context(4.0F, 0.0F, false, "minecraft:shield", 45,
                List.of(item(12, 9, "minecraft:totem_of_undying"))));
        assertTrue(equip.isPresent());

        var restore = module.nextAction(4L, new AutoTotem.Context(20.0F, 0.0F, true,
                "minecraft:totem_of_undying", 45, List.of(item(12, 9, "minecraft:shield"))));
        assertTrue(restore.isPresent());
        assertEquals(12, restore.get().getFirst().slot());
    }

    @Test
    void totemRestoresAnOriginallyEmptyOffhandOnlyWhenItsRecordedSourceIsStillEmpty() {
        AutoTotem module = new AutoTotem();
        module.enable();
        assertTrue(module.nextAction(0L, new AutoTotem.Context(4.0F, 0.0F, false, "", 45,
                List.of(item(12, 9, "minecraft:totem_of_undying")))).isPresent());

        var restore = module.nextAction(4L, new AutoTotem.Context(20.0F, 0.0F, true,
                "minecraft:totem_of_undying", 45, List.of()));
        assertTrue(restore.isPresent());
        assertEquals(45, restore.get().get(1).slot());
        assertEquals(12, restore.get().getLast().slot());
    }

    @Test
    void inventoryPolicyPrioritizesSafePreferredHotbarPlacement() {
        var options = new InventoryManagerPolicy.Options(true, false, Set.of(), Map.of("minecraft:torch", 8),
                true, true, 8);
        var result = InventoryManagerPolicy.nextAction(List.of(item(18, 9, "minecraft:torch")),
                Map.of(8, 44, 9, 18), options);

        assertTrue(result.isPresent());
        assertEquals(InventoryManagerPolicy.ActionType.SWAP, result.get().type());
        assertEquals(44, result.get().destinationMenuSlot());
    }

    @Test
    void inventoryPolicyDoesNotDropProtectedNamedJunk() {
        var namedJunk = new InventoryItem(18, 9, "minecraft:rotten_flesh", 1, true, false, 0);
        var options = new InventoryManagerPolicy.Options(false, true, Set.of("minecraft:rotten_flesh"), Map.of(),
                true, true, 0);

        assertFalse(InventoryManagerPolicy.nextAction(List.of(namedJunk), Map.of(9, 18), options).isPresent());
    }

    private static InventoryItem item(int menuSlot, int inventorySlot, String itemId) {
        return new InventoryItem(menuSlot, inventorySlot, itemId, 1, false, false, 0);
    }
}
