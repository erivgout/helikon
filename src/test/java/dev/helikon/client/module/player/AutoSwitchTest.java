package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.ItemSelectorSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoSwitchTest {
    @Test
    void identityAndDefaultsAreSafe() {
        AutoSwitch module = new AutoSwitch();

        assertEquals("auto_switch", module.id());
        assertEquals("AutoSwitch", module.name());
        assertEquals(ModuleCategory.PLAYER, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(AutoSwitch.Trigger.ATTACK_HELD, triggerSetting(module).value());
        assertTrue(booleanSetting(module, "restore_prior_slot").value());
        assertEquals(AutoSwitch.Action.none(), module.update(context(0, true, false,
                item(2, "minecraft:diamond_sword"))));
    }

    @Test
    void selectsFirstConfiguredAvailableItemDuringTrigger() {
        AutoSwitch module = enabledModule();
        AutoSwitch.Action action = module.update(context(0, true, false,
                item(1, "minecraft:iron_sword"),
                item(5, "minecraft:diamond_sword"),
                item(2, "minecraft:diamond_sword")));

        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2), action);
        assertEquals(AutoSwitch.Action.none(), module.update(context(2, true, false,
                item(2, "minecraft:diamond_sword"))));
    }

    @Test
    void restoresOnlyWhileItStillOwnsTheSelectedSlot() {
        AutoSwitch module = enabledModule();
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2),
                module.update(context(0, true, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.RESTORE, 0),
                module.update(context(2, false, false, item(2, "minecraft:diamond_sword"))));

        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2),
                module.update(context(0, true, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(AutoSwitch.Action.none(),
                module.update(context(4, false, false, item(2, "minecraft:diamond_sword"))));
    }

    @Test
    void disableRequestsOwnershipSafeRestore() {
        AutoSwitch module = enabledModule();
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2),
                module.update(context(0, true, false, item(2, "minecraft:diamond_sword"))));

        module.disable();
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.RESTORE, 0),
                module.update(context(2, false, false, item(2, "minecraft:diamond_sword"))));
    }

    @Test
    void manualChoiceSuspendsSwitchingUntilTriggerEnds() {
        AutoSwitch module = enabledModule();
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2),
                module.update(context(0, true, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(AutoSwitch.Action.none(),
                module.update(context(4, true, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(AutoSwitch.Action.none(),
                module.update(context(4, true, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(AutoSwitch.Action.none(),
                module.update(context(4, false, false, item(2, "minecraft:diamond_sword"))));
        assertEquals(new AutoSwitch.Action(AutoSwitch.ActionType.SELECT, 2),
                module.update(context(4, true, false, item(2, "minecraft:diamond_sword"))));
    }

    @Test
    void supportsConfiguredUseSneakHealthAndAlwaysConditions() {
        AutoSwitch module = enabledModule();
        setTrigger(module, AutoSwitch.Trigger.USE_HELD);
        assertEquals(AutoSwitch.ActionType.SELECT, module.update(context(0, false, true,
                item(2, "minecraft:diamond_sword"))).type());
        module.onPlayerUnavailable();

        setTrigger(module, AutoSwitch.Trigger.SNEAKING);
        assertEquals(AutoSwitch.ActionType.SELECT, module.update(new AutoSwitch.Context(
                0, false, false, false, true, 20.0D,
                List.of(item(2, "minecraft:diamond_sword")))).type());
        module.onPlayerUnavailable();

        setTrigger(module, AutoSwitch.Trigger.LOW_HEALTH);
        assertEquals(AutoSwitch.ActionType.SELECT, module.update(new AutoSwitch.Context(
                0, false, false, false, false, 10.0D,
                List.of(item(2, "minecraft:diamond_sword")))).type());
        module.onPlayerUnavailable();

        setTrigger(module, AutoSwitch.Trigger.ALWAYS);
        assertEquals(AutoSwitch.ActionType.SELECT, module.update(context(0, false, false,
                item(2, "minecraft:diamond_sword"))).type());
    }

    @Test
    void screenMissingItemsAndInvalidFactsAreSafe() {
        AutoSwitch module = enabledModule();
        assertEquals(AutoSwitch.Action.none(), module.update(new AutoSwitch.Context(
                0, true, true, false, false, 20.0D,
                List.of(item(2, "minecraft:diamond_sword")))));
        assertEquals(AutoSwitch.Action.none(), module.update(context(0, true, false)));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoSwitch.HotbarItem(9, "minecraft:stone"));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoSwitch.Context(0, false, false, false,
                        false, Double.NaN, List.of()));
    }

    @Test
    void settingsValidateBoundsAndIdentifiers() {
        AutoSwitch module = new AutoSwitch();
        ItemSelectorSetting items = itemSetting(module);
        NumberSetting threshold = numberSetting(module, "health_threshold");

        assertEquals(16, items.maximumEntries());
        assertEquals(1.0D, threshold.minimum());
        assertEquals(20.0D, threshold.maximum());
        assertThrows(IllegalArgumentException.class, () -> items.set(List.of("not an item")));
        assertThrows(IllegalArgumentException.class, () -> threshold.set(20.1D));
    }

    private static AutoSwitch.Context context(int currentSlot, boolean attackHeld, boolean useHeld,
                                               AutoSwitch.HotbarItem... items) {
        return new AutoSwitch.Context(currentSlot, false, attackHeld, useHeld,
                false, 20.0D, List.of(items));
    }

    private static AutoSwitch.HotbarItem item(int slot, String itemId) {
        return new AutoSwitch.HotbarItem(slot, itemId);
    }

    private static AutoSwitch enabledModule() {
        AutoSwitch module = new AutoSwitch();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<AutoSwitch.Trigger> triggerSetting(AutoSwitch module) {
        return (EnumSetting<AutoSwitch.Trigger>) module.settings().stream()
                .filter(setting -> setting.id().equals("trigger")).findFirst().orElseThrow();
    }

    private static void setTrigger(AutoSwitch module, AutoSwitch.Trigger trigger) {
        triggerSetting(module).set(trigger);
    }

    private static BooleanSetting booleanSetting(AutoSwitch module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static ItemSelectorSetting itemSetting(AutoSwitch module) {
        return (ItemSelectorSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("target_items")).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(AutoSwitch module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
