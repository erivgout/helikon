package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class HitSwapTest {
    @Test
    void defaultsOffWithFirstHotbarSlotConfigured() {
        HitSwap hitSwap = new HitSwap();

        assertEquals("hit_swap", hitSwap.id());
        assertFalse(hitSwap.defaultEnabled());
        assertEquals(1, hitSwap.weaponSlot());
        assertEquals(4, hitSwap.holdTicks());
        assertEquals(HitSwap.Action.none(), hitSwap.beforeAttack(4, true));
    }

    @Test
    void selectsConfiguredOccupiedSlotAndRestoresThePriorSlot() {
        HitSwap hitSwap = enabledModule();
        integerSetting(hitSwap, "weapon_slot").set(4);
        integerSetting(hitSwap, "hold_ticks").set(1);

        assertEquals(new HitSwap.Action(HitSwap.ActionType.SELECT, 3), hitSwap.beforeAttack(7, true));
        assertEquals(HitSwap.Action.none(), hitSwap.beforeAttack(3, true));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(3));
        assertEquals(new HitSwap.Action(HitSwap.ActionType.RESTORE, 7), hitSwap.restore(3));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(7));
    }

    @Test
    void skipsEmptyOrAlreadySelectedConfiguredSlot() {
        HitSwap hitSwap = enabledModule();
        integerSetting(hitSwap, "weapon_slot").set(2);

        assertEquals(HitSwap.Action.none(), hitSwap.beforeAttack(0, false));
        assertEquals(HitSwap.Action.none(), hitSwap.beforeAttack(1, true));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(1));
    }

    @Test
    void holdsTheWeaponForConfiguredTicksAfterTheLatestAttack() {
        HitSwap hitSwap = enabledModule();
        integerSetting(hitSwap, "weapon_slot").set(2);
        integerSetting(hitSwap, "hold_ticks").set(2);

        assertEquals(new HitSwap.Action(HitSwap.ActionType.SELECT, 1), hitSwap.beforeAttack(0, true));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(1));
        assertEquals(HitSwap.Action.none(), hitSwap.beforeAttack(1, true));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(1));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(1));
        assertEquals(new HitSwap.Action(HitSwap.ActionType.RESTORE, 0), hitSwap.restore(1));
    }

    @Test
    void neverRestoresOverAManualSlotChangeAndClearsOnWorldLoss() {
        HitSwap hitSwap = enabledModule();
        integerSetting(hitSwap, "weapon_slot").set(2);

        assertEquals(new HitSwap.Action(HitSwap.ActionType.SELECT, 1), hitSwap.beforeAttack(0, true));
        assertEquals(HitSwap.Action.none(), hitSwap.restore(5));

        assertEquals(new HitSwap.Action(HitSwap.ActionType.SELECT, 1), hitSwap.beforeAttack(0, true));
        hitSwap.onPlayerUnavailable();
        assertEquals(HitSwap.Action.none(), hitSwap.restore(1));
    }

    @Test
    void restoresOwnedSelectionAfterDisableAndValidatesSlotsAndSettingBounds() {
        HitSwap hitSwap = enabledModule();
        IntegerSetting weaponSlot = integerSetting(hitSwap, "weapon_slot");
        weaponSlot.set(9);
        assertEquals(new HitSwap.Action(HitSwap.ActionType.SELECT, 8), hitSwap.beforeAttack(2, true));

        hitSwap.disable();
        assertEquals(new HitSwap.Action(HitSwap.ActionType.RESTORE, 2), hitSwap.restore(8));
        assertEquals(1, weaponSlot.minimum());
        assertEquals(9, weaponSlot.maximum());
        assertThrows(IllegalArgumentException.class, () -> weaponSlot.set(0));
        assertThrows(IllegalArgumentException.class, () -> hitSwap.beforeAttack(9, true));
    }

    private static HitSwap enabledModule() {
        HitSwap hitSwap = new HitSwap();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(hitSwap);
        registry.setEnabled(hitSwap, true);
        return hitSwap;
    }

    private static IntegerSetting integerSetting(HitSwap module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
