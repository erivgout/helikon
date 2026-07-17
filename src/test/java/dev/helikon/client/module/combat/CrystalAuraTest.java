package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrystalAuraTest {
    @Test
    void identityAndDefaultsMatchCombatAdvantagePolicy() {
        CrystalAura module = new CrystalAura();
        assertEquals("crystal_aura", module.id());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled(), "advantage modules must default off");
        assertTrue(module.excludeFriends());
        assertTrue(module.allowPlayers());
        assertFalse(module.allowHostiles());
    }

    @Test
    void disabledModuleNeverActs() {
        CrystalAura module = new CrystalAura();
        CrystalAura.State state = new CrystalAura.State(true, true, true, true,
                List.of(new CrystalAura.Placement(0, 63, 0, 3.0D, 2.0D)),
                List.of(new CrystalAura.Crystal("c", 3.0D, 2.0D)));
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, state).type());
    }

    @Test
    void prefersDetonatingAnExistingCrystalOverPlacing() {
        CrystalAura module = enabled(new CrystalAura());
        CrystalAura.State state = new CrystalAura.State(true, true, true, true,
                List.of(new CrystalAura.Placement(1, 63, 1, 3.0D, 2.0D)),
                List.of(new CrystalAura.Crystal("near", 3.0D, 1.0D)));
        CrystalAura.Action action = module.decide(0L, state);
        assertEquals(CrystalAura.ActionType.ATTACK, action.type());
        assertEquals("near", action.crystalId());
    }

    @Test
    void placesTheCandidateClosestToATargetWhenNoCrystalExists() {
        CrystalAura module = enabled(new CrystalAura());
        CrystalAura.Placement far = new CrystalAura.Placement(5, 63, 0, 3.0D, 3.5D);
        CrystalAura.Placement near = new CrystalAura.Placement(1, 63, 1, 4.0D, 1.5D);
        CrystalAura.State state = new CrystalAura.State(true, true, true, true, List.of(far, near), List.of());
        CrystalAura.Action action = module.decide(0L, state);
        assertEquals(CrystalAura.ActionType.PLACE, action.type());
        assertEquals(1, action.x());
        assertEquals(1, action.z());
    }

    @Test
    void honorsRangesDamageRadiusHoldingAndPlaceReadiness() {
        CrystalAura module = enabled(new CrystalAura());
        // Crystal out of damage radius (default 4) and placement out of place range (default 4.5).
        CrystalAura.State outOfBounds = new CrystalAura.State(true, true, true, true,
                List.of(new CrystalAura.Placement(9, 63, 0, 9.0D, 2.0D)),
                List.of(new CrystalAura.Crystal("c", 3.0D, 5.0D)));
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, outOfBounds).type());

        // A valid placement is ignored while the player is not holding a crystal or place is not ready.
        CrystalAura.State notHolding = new CrystalAura.State(false, true, true, true,
                List.of(new CrystalAura.Placement(1, 63, 1, 3.0D, 2.0D)), List.of());
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, notHolding).type());
        CrystalAura.State placeNotReady = new CrystalAura.State(true, false, true, true,
                List.of(new CrystalAura.Placement(1, 63, 1, 3.0D, 2.0D)), List.of());
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, placeNotReady).type());
    }

    @Test
    void requiresTargetAttackReadinessAndBoundedDelayBetweenActions() {
        CrystalAura module = enabled(new CrystalAura());
        List<CrystalAura.Crystal> crystals = List.of(new CrystalAura.Crystal("c", 3.0D, 1.0D));
        CrystalAura.State noTarget = new CrystalAura.State(true, true, true, false, List.of(), crystals);
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, noTarget).type());

        CrystalAura.State notReady = new CrystalAura.State(true, true, false, true, List.of(), crystals);
        assertEquals(CrystalAura.ActionType.NONE, module.decide(0L, notReady).type());

        CrystalAura.State ready = new CrystalAura.State(true, true, true, true, List.of(), crystals);
        assertEquals(CrystalAura.ActionType.ATTACK, module.decide(0L, ready).type());
        // Default delay is 2 ticks, so the next tick is suppressed and tick 2 acts again.
        assertEquals(CrystalAura.ActionType.NONE, module.decide(1L, ready).type());
        assertEquals(CrystalAura.ActionType.ATTACK, module.decide(2L, ready).type());
    }

    @Test
    void disableResetsTheActionCadence() {
        CrystalAura module = enabled(new CrystalAura());
        List<CrystalAura.Crystal> crystals = List.of(new CrystalAura.Crystal("c", 3.0D, 1.0D));
        CrystalAura.State ready = new CrystalAura.State(true, true, true, true, List.of(), crystals);
        assertEquals(CrystalAura.ActionType.ATTACK, module.decide(10L, ready).type());
        module.disable();
        module.enable();
        // After a clean disable the cadence is cleared, so an earlier tick may act again.
        assertEquals(CrystalAura.ActionType.ATTACK, module.decide(0L, ready).type());
    }

    @Test
    void detonatePlaceTogglesGateEachActionKind() {
        CrystalAura module = enabled(new CrystalAura());
        booleanSetting(module, "detonate").set(false);
        CrystalAura.State state = new CrystalAura.State(true, true, true, true,
                List.of(new CrystalAura.Placement(1, 63, 1, 3.0D, 2.0D)),
                List.of(new CrystalAura.Crystal("c", 3.0D, 1.0D)));
        // Detonate disabled: it falls through to placement instead of attacking.
        assertEquals(CrystalAura.ActionType.PLACE, module.decide(0L, state).type());
    }

    private static CrystalAura enabled(CrystalAura module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(CrystalAura module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst()
                .orElseThrow();
    }
}
