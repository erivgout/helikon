package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoAnchorTest {
    private static final double NO_FRIEND = Double.MAX_VALUE;

    @Test
    void identityAndDefaultsMatchCombatSafetyPolicy() {
        AutoAnchor module = new AutoAnchor();

        assertEquals("auto_anchor", module.id());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.excludeFriends());
        assertTrue(module.allowPlayers());
        assertFalse(module.allowHostiles());
    }

    @Test
    void disabledModuleNeverActs() {
        AutoAnchor module = new AutoAnchor();

        assertEquals(AutoAnchor.Action.none(), module.decide(0L, state(
                List.of(anchor(1, 3.5D, 2.0D, NO_FRIEND, true)),
                List.of(placement(3.5D, 2.0D, NO_FRIEND, true))
        )));
    }

    @Test
    void detonatesChargedAnchorBeforeChargingOrPlacing() {
        AutoAnchor module = enabledModule();
        AutoAnchor.Action action = module.decide(0L, state(
                List.of(anchor(1, 3.5D, 2.0D, NO_FRIEND, true)),
                List.of(placement(3.5D, 1.0D, NO_FRIEND, true))
        ));

        assertEquals(AutoAnchor.ActionType.DETONATE, action.type());
        assertEquals(2, action.hotbarSlot());
    }

    @Test
    void chargesThenPlacesWhenLaterStagesAreUnavailable() {
        AutoAnchor module = enabledModule();
        AutoAnchor.Action charge = module.decide(0L, state(
                List.of(anchor(0, 3.5D, 2.0D, NO_FRIEND, true)),
                List.of(placement(3.5D, 1.0D, NO_FRIEND, true))
        ));
        assertEquals(AutoAnchor.ActionType.CHARGE, charge.type());
        assertEquals(1, charge.hotbarSlot());

        module.disable();
        module.enable();
        AutoAnchor.State noExistingAnchor = new AutoAnchor.State(true, true, 0, 1, 2,
                List.of(), List.of(placement(3.5D, 1.0D, NO_FRIEND, true)));
        assertEquals(AutoAnchor.ActionType.PLACE, module.decide(0L, noExistingAnchor).type());
    }

    @Test
    void refusesNonExplosiveUnsafeOrFriendAdjacentCandidates() {
        AutoAnchor module = enabledModule();
        List<AutoAnchor.Anchor> rejected = List.of(
                anchor(1, 3.5D, 2.0D, NO_FRIEND, false),
                anchor(1, 2.5D, 2.0D, NO_FRIEND, true),
                anchor(1, 3.5D, 2.0D, 3.0D, true),
                anchor(1, 3.5D, 5.0D, NO_FRIEND, true)
        );

        assertEquals(AutoAnchor.Action.none(), module.decide(0L,
                new AutoAnchor.State(true, true, -1, -1, 2, rejected, List.of())));

        booleanSetting(module, "exclude_friends").set(false);
        assertEquals(AutoAnchor.ActionType.DETONATE, module.decide(0L,
                new AutoAnchor.State(true, true, -1, -1, 2,
                        List.of(anchor(1, 3.5D, 2.0D, 3.0D, true)), List.of())).type());
    }

    @Test
    void honorsChargeGoalResourcesReadinessAndCadence() {
        AutoAnchor module = enabledModule();
        integerSetting(module, "detonate_at_charges").set(2);
        AutoAnchor.State oneCharge = state(List.of(anchor(1, 3.5D, 2.0D, NO_FRIEND, true)), List.of());
        assertEquals(AutoAnchor.ActionType.CHARGE, module.decide(0L, oneCharge).type());
        assertEquals(AutoAnchor.Action.none(), module.decide(3L, oneCharge));

        AutoAnchor.State twoCharges = state(List.of(anchor(2, 3.5D, 2.0D, NO_FRIEND, true)), List.of());
        assertEquals(AutoAnchor.ActionType.DETONATE, module.decide(4L, twoCharges).type());

        module.disable();
        module.enable();
        assertEquals(AutoAnchor.Action.none(), module.decide(0L,
                new AutoAnchor.State(false, true, 0, 1, 2, List.of(), List.of(
                        placement(3.5D, 1.0D, NO_FRIEND, true)))));
        assertEquals(AutoAnchor.Action.none(), module.decide(0L,
                new AutoAnchor.State(true, false, 0, 1, 2, List.of(), List.of(
                        placement(3.5D, 1.0D, NO_FRIEND, true)))));
    }

    @Test
    void validatesSettingAndFactBounds() {
        AutoAnchor module = new AutoAnchor();
        IntegerSetting charges = integerSetting(module, "detonate_at_charges");
        NumberSetting interactRange = numberSetting(module, "interact_range");

        assertEquals(1, charges.minimum());
        assertEquals(4, charges.maximum());
        assertEquals(1.0D, interactRange.minimum());
        assertEquals(6.0D, interactRange.maximum());
        assertThrows(IllegalArgumentException.class, () -> charges.set(5));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoAnchor.Anchor(0, 0, 0, 5, 3.0D, 2.0D, NO_FRIEND, true));
        assertThrows(IllegalArgumentException.class,
                () -> new AutoAnchor.State(true, true, 9, 1, 2, List.of(), List.of()));
    }

    private static AutoAnchor.State state(List<AutoAnchor.Anchor> anchors, List<AutoAnchor.Placement> placements) {
        return new AutoAnchor.State(true, true, 0, 1, 2, anchors, placements);
    }

    private static AutoAnchor.Anchor anchor(int charges, double playerDistance, double targetDistance,
                                            double friendDistance, boolean explosive) {
        return new AutoAnchor.Anchor(1, 64, 2, charges, playerDistance, targetDistance, friendDistance, explosive);
    }

    private static AutoAnchor.Placement placement(double playerDistance, double targetDistance,
                                                   double friendDistance, boolean explosive) {
        return new AutoAnchor.Placement(1, 64, 2, playerDistance, targetDistance, friendDistance, explosive);
    }

    private static AutoAnchor enabledModule() {
        AutoAnchor module = new AutoAnchor();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(AutoAnchor module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integerSetting(AutoAnchor module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(AutoAnchor module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
