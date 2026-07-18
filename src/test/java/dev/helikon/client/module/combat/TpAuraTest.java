package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TpAuraTest {
    @Test
    void defaultsOffAndIdentifiesAsCombatModule() {
        TpAura aura = new TpAura();

        assertEquals("tp_aura", aura.id());
        assertEquals(ModuleCategory.COMBAT, aura.category());
        assertFalse(aura.defaultEnabled());
        assertTrue(aura.nextPlan(0L, true, List.of(target("hostile", CombatEntityType.HOSTILE,
                false, true, 8.0D))).isEmpty());
    }

    @Test
    void selectsNearestVisibleNonFriendWithinBoundedTravel() {
        TpAura aura = enabled(new TpAura());
        CombatTarget friend = target("friend", CombatEntityType.PLAYER, true, true, 5.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, 4.0D);
        CombatTarget farther = target("farther", CombatEntityType.HOSTILE, false, true, 9.0D);
        CombatTarget nearest = target("nearest", CombatEntityType.HOSTILE, false, true, 6.0D);

        TpAura.AttackPlan plan = aura.nextPlan(0L, true,
                List.of(friend, blocked, farther, nearest)).orElseThrow();

        assertEquals("nearest", plan.target().id());
        assertEquals(2.5D, plan.attackDistance(), 0.0D);
        assertEquals(6, plan.maximumSteps());
    }

    @Test
    void selectsClosePassiveTargetsForDirectAttackByDefault() {
        TpAura aura = enabled(new TpAura());

        TpAura.AttackPlan plan = aura.nextPlan(0L, true,
                List.of(target("horse", CombatEntityType.PASSIVE, false, true, 2.0D))).orElseThrow();

        assertEquals("horse", plan.target().id());
    }

    @Test
    void pathUsesBoundedStepsAndEndsExactlyAtDestination() {
        TpAura aura = enabled(new TpAura());
        TpAura.AttackPlan plan = aura.nextPlan(0L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).orElseThrow();
        TpAura.Point from = new TpAura.Point(0.0D, 64.0D, 0.0D);
        TpAura.Point to = new TpAura.Point(7.0D, 64.0D, 0.0D);

        List<TpAura.Point> path = aura.buildPath(from, to, plan);

        assertEquals(3, path.size());
        assertEquals(to, path.getLast());
        assertTrue(distance(from, path.getFirst()) <= plan.maximumStepDistance());
        assertTrue(distance(path.get(0), path.get(1)) <= plan.maximumStepDistance());
    }

    @Test
    void orbitDestinationAdvancesAroundTargetInsteadOfReturningAway() {
        TpAura aura = enabled(new TpAura());
        TpAura.Point target = new TpAura.Point(10.0D, 64.0D, 10.0D);
        TpAura.Point west = new TpAura.Point(5.0D, 64.0D, 10.0D);

        TpAura.Point first = aura.orbitDestination(west, target, 2.5D);
        TpAura.Point second = aura.orbitDestination(first, target, 2.5D);

        assertEquals(10.0D, first.x(), 1.0E-9D);
        assertEquals(7.5D, first.z(), 1.0E-9D);
        assertEquals(12.5D, second.x(), 1.0E-9D);
        assertEquals(10.0D, second.z(), 1.0E-9D);
        assertEquals(2.5D, distance(first, target), 1.0E-9D);
        assertEquals(2.5D, distance(second, target), 1.0E-9D);
    }

    @Test
    void rejectsPathBeyondPacketCapAndAppliesCooldownOnlyAfterExecution() {
        TpAura aura = enabled(new TpAura());
        TpAura.AttackPlan plan = aura.nextPlan(0L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).orElseThrow();

        assertTrue(aura.buildPath(new TpAura.Point(0.0D, 0.0D, 0.0D),
                new TpAura.Point(20.0D, 0.0D, 0.0D), plan).isEmpty());
        assertTrue(aura.nextPlan(1L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).isPresent());
        aura.markExecuted(1L);
        assertTrue(aura.nextPlan(2L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).isEmpty());
        assertTrue(aura.nextPlan(11L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).isPresent());
    }

    @Test
    void disableResetsCooldownAndSettingsEnforceBounds() {
        TpAura aura = enabled(new TpAura());
        aura.markExecuted(10L);
        aura.disable();
        aura.enable();
        assertTrue(aura.nextPlan(0L, true,
                List.of(target("target", CombatEntityType.HOSTILE, false, true, 8.0D))).isPresent());

        NumberSetting range = (NumberSetting) setting(aura, "range");
        IntegerSetting maximumSteps = (IntegerSetting) setting(aura, "maximum_steps");
        BooleanSetting returnToOrigin = (BooleanSetting) setting(aura, "return_to_origin");
        IntegerSetting returnDelay = (IntegerSetting) setting(aura, "return_delay_ticks");
        assertFalse(aura.returnsToOrigin());
        returnToOrigin.set(true);
        assertTrue(aura.returnsToOrigin());
        assertEquals(3, aura.returnDelayTicks());
        assertEquals(1, returnDelay.minimum());
        assertEquals(10, returnDelay.maximum());
        assertThrows(IllegalArgumentException.class, () -> range.set(25.0D));
        assertThrows(IllegalArgumentException.class, () -> maximumSteps.set(13));
        assertThrows(IllegalArgumentException.class,
                () -> new TpAura.Point(Double.NaN, 0.0D, 0.0D));
    }

    private static double distance(TpAura.Point first, TpAura.Point second) {
        return Math.sqrt(Math.pow(second.x() - first.x(), 2.0D)
                + Math.pow(second.y() - first.y(), 2.0D)
                + Math.pow(second.z() - first.z(), 2.0D));
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend,
                                       boolean lineOfSight, double distance) {
        return new CombatTarget(id, id, type, friend, false, true, true, lineOfSight,
                distance, 0.0D, distance, 0.0D, 0.0D,
                0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:air", List.of());
    }

    private static Setting<?> setting(TpAura module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static TpAura enabled(TpAura module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
