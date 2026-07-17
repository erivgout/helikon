package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClutchTest {
    @Test
    void disabledModuleNeverActs() {
        Clutch clutch = new Clutch();
        assertEquals(Clutch.Action.NONE, clutch.plan(0L, descending(3.0D, 2.0D, true, true)));
    }

    @Test
    void bothModePrefersWaterThenBlockWhenCloseToGround() {
        Clutch clutch = enabled(new Clutch());
        // Water available: chosen over a block.
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(0L, descending(3.0D, 2.0D, true, true)));
        // Only a block available on a fresh module.
        Clutch fresh = enabled(new Clutch());
        assertEquals(Clutch.Action.PLACE_BLOCK, fresh.plan(0L, descending(3.0D, 2.0D, false, true)));
        // Neither item available: nothing to clutch with.
        Clutch empty = enabled(new Clutch());
        assertEquals(Clutch.Action.NONE, empty.plan(0L, descending(3.0D, 2.0D, false, false)));
    }

    @Test
    void modeRestrictsTheChosenInteraction() {
        // Block mode ignores an available water bucket when no block is held.
        Clutch blockOnly = enabled(new Clutch());
        enumSetting(blockOnly).set(Clutch.Mode.BLOCK);
        assertEquals(Clutch.Action.NONE, blockOnly.plan(0L, descending(3.0D, 2.0D, true, false)));
        assertEquals(Clutch.Action.PLACE_BLOCK, enabled(withMode(Clutch.Mode.BLOCK))
                .plan(0L, descending(3.0D, 2.0D, true, true)));

        // Water mode ignores an available block when no water bucket is held.
        Clutch waterOnly = enabled(withMode(Clutch.Mode.WATER));
        assertEquals(Clutch.Action.NONE, waterOnly.plan(0L, descending(3.0D, 2.0D, false, true)));
        assertEquals(Clutch.Action.USE_WATER, enabled(withMode(Clutch.Mode.WATER))
                .plan(0L, descending(3.0D, 2.0D, true, true)));
    }

    @Test
    void ignoresStatesWhereClutchingIsUnsafeOrUnneeded() {
        Clutch clutch = enabled(new Clutch());
        // Not descending.
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(false, false, false, false, false, false, 5.0D, 2.0D, true, true)));
        // On ground.
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(true, true, false, false, false, false, 5.0D, 2.0D, true, true)));
        // Riding, flying, fall-flying, or in liquid.
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(true, false, true, false, false, false, 5.0D, 2.0D, true, true)));
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(true, false, false, true, false, false, 5.0D, 2.0D, true, true)));
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(true, false, false, false, true, false, 5.0D, 2.0D, true, true)));
        assertEquals(Clutch.Action.NONE, clutch.plan(0L,
                new Clutch.State(true, false, false, false, false, true, 5.0D, 2.0D, true, true)));
    }

    @Test
    void respectsFallThresholdAndActivationDistance() {
        Clutch clutch = enabled(new Clutch());
        // Fall too short (default minimum 3.0).
        assertEquals(Clutch.Action.NONE, clutch.plan(0L, descending(2.5D, 2.0D, true, true)));
        // Ground too far away (default activation distance 3.0).
        assertEquals(Clutch.Action.NONE, clutch.plan(0L, descending(10.0D, 4.0D, true, true)));
        // Exactly at the boundaries still acts.
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(0L, descending(3.0D, 3.0D, true, true)));
    }

    @Test
    void placementDelayBoundsConsecutiveRequests() {
        Clutch clutch = enabled(new Clutch());
        numberSetting(clutch, "placement_delay_ticks").set(5.0D);
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(0L, descending(5.0D, 2.0D, true, true)));
        assertEquals(Clutch.Action.NONE, clutch.plan(4L, descending(5.0D, 2.0D, true, true)));
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(5L, descending(5.0D, 2.0D, true, true)));
    }

    @Test
    void selectSlotPicksMatchingItemOnlyWhenNeeded() {
        Clutch clutch = enabled(new Clutch());
        List<Clutch.HotbarItem> items = List.of(
                new Clutch.HotbarItem(2, true, false),
                new Clutch.HotbarItem(5, false, true),
                new Clutch.HotbarItem(7, true, false));
        assertEquals(2, clutch.selectSlot(Clutch.Action.PLACE_BLOCK, 0, false, items).orElseThrow());
        assertEquals(5, clutch.selectSlot(Clutch.Action.USE_WATER, 0, false, items).orElseThrow());
        // Already holding a matching item: no switch.
        assertTrue(clutch.selectSlot(Clutch.Action.PLACE_BLOCK, 2, true, items).isEmpty());
        // No matching candidate.
        assertTrue(clutch.selectSlot(Clutch.Action.USE_WATER, 0, false,
                List.of(new Clutch.HotbarItem(2, true, false))).isEmpty());
    }

    @Test
    void disableRestoresTheDelaySoAReenableCanActImmediately() {
        Clutch clutch = enabled(new Clutch());
        numberSetting(clutch, "placement_delay_ticks").set(20.0D);
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(0L, descending(5.0D, 2.0D, true, true)));
        clutch.disable();
        clutch.enable();
        assertEquals(Clutch.Action.USE_WATER, clutch.plan(0L, descending(5.0D, 2.0D, true, true)));
    }

    private static Clutch.State descending(double fallDistance, double spaceBelow, boolean hasWater, boolean hasBlock) {
        return new Clutch.State(true, false, false, false, false, false, fallDistance, spaceBelow, hasBlock, hasWater);
    }

    private static Clutch withMode(Clutch.Mode mode) {
        Clutch clutch = new Clutch();
        enumSetting(clutch).set(mode);
        return clutch;
    }

    private static Clutch enabled(Clutch clutch) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(clutch);
        registry.setEnabled(clutch, true);
        return clutch;
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<Clutch.Mode> enumSetting(Clutch clutch) {
        return (EnumSetting<Clutch.Mode>) clutch.settings().stream()
                .filter(setting -> setting.id().equals("mode")).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Clutch clutch, String id) {
        return (NumberSetting) clutch.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
