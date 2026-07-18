package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GojosInfinityTest {
    @Test
    void defaultsOffWithHonestRepelSettings() {
        GojosInfinity module = new GojosInfinity();

        assertEquals("gojo_infinity", module.id());
        assertEquals("Gojo's Infinity", module.name());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.plan(0L, 1.0D, List.of(threat("target", 2.0D, 0.2D, 10.0D))).isEmpty());

        NumberSetting detection = numberSetting(module, "detection_radius");
        NumberSetting repel = numberSetting(module, "repel_distance");
        IntegerSetting targets = integerSetting(module, "targets_per_tick");
        assertEquals(6.0D, detection.value());
        assertEquals(3.0D, repel.value());
        assertEquals(1, targets.value());
        assertEquals(4, targets.maximum());
    }

    @Test
    void strongestModeChoosesEarliestLegalApproachingThreat() {
        GojosInfinity module = enabled();
        GojosInfinity.Threat fartherSooner = threat("soon", 2.8D, 0.7D, 4.0D);
        GojosInfinity.Threat nearerLater = threat("later", 2.0D, 0.1D, 20.0D);
        GojosInfinity.Threat outOfRange = new GojosInfinity.Threat("illegal",
                GojosInfinity.TargetKind.HOSTILE, false, false, false, false,
                true, true, false, 1.0D, 1.0D, 1.0D);

        GojosInfinity.AttackPlan plan = module.plan(0L, 1.0D,
                List.of(nearerLater, outOfRange, fartherSooner)).orElseThrow();

        assertEquals(List.of("soon"), plan.targetIds());
        assertTrue(plan.sprintReset());
        assertTrue(plan.silentRotation());
    }

    @Test
    void filtersFriendsPetsArmorStandsAndRetreatingTargets() {
        GojosInfinity module = enabled();
        GojosInfinity.Threat friend = changed(threat("friend", 2.0D, 0.2D, 10.0D),
                true, false, false, 0.2D);
        GojosInfinity.Threat pet = changed(threat("pet", 2.0D, 0.2D, 10.0D),
                false, true, false, 0.2D);
        GojosInfinity.Threat armorStand = changed(threat("stand", 2.0D, 0.2D, 10.0D),
                false, false, true, 0.2D);
        GojosInfinity.Threat retreating = changed(threat("away", 2.0D, 0.2D, 10.0D),
                false, false, false, -0.2D);

        assertTrue(module.plan(0L, 1.0D,
                List.of(friend, pet, armorStand, retreating)).isEmpty());
    }

    @Test
    void crowdModeIsBoundedAndCadenceConsumesOnlyAfterExecution() {
        GojosInfinity module = enabled();
        enumSetting(module, "repel_mode").set(GojosInfinity.RepelMode.CROWD_EMERGENCY);
        integerSetting(module, "targets_per_tick").set(3);
        integerSetting(module, "attack_interval").set(4);
        List<GojosInfinity.Threat> threats = List.of(
                threat("a", 2.0D, 0.2D, 3.0D),
                threat("b", 2.0D, 0.2D, 2.0D),
                threat("c", 2.0D, 0.2D, 1.0D),
                threat("d", 2.0D, 0.2D, 4.0D));

        assertEquals(List.of("c", "b", "a"),
                module.plan(0L, 1.0D, threats).orElseThrow().targetIds());
        assertTrue(module.plan(1L, 1.0D, threats).isPresent());
        module.markExecuted(1L);
        assertTrue(module.plan(4L, 1.0D, threats).isEmpty());
        assertTrue(module.plan(5L, 1.0D, threats).isPresent());
    }

    @Test
    void attackChargeAndFactBoundsAreValidated() {
        GojosInfinity module = enabled();
        assertTrue(module.plan(0L, 0.89D, List.of(threat("target", 2.0D, 0.2D, 1.0D))).isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> module.plan(0L, Double.NaN, List.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new GojosInfinity.Threat("", GojosInfinity.TargetKind.PLAYER,
                        false, false, false, false, true, true, true,
                        1.0D, 0.1D, 1.0D));
    }

    private static GojosInfinity.Threat threat(String id, double distance,
                                               double closingSpeed, double impactTicks) {
        return new GojosInfinity.Threat(id, GojosInfinity.TargetKind.HOSTILE,
                false, false, false, false, true, true, true,
                distance, closingSpeed, impactTicks);
    }

    private static GojosInfinity.Threat changed(GojosInfinity.Threat source,
                                                boolean friend, boolean pet, boolean armorStand,
                                                double closingSpeed) {
        return new GojosInfinity.Threat(source.id(), source.kind(), friend, pet, armorStand,
                source.suspectedBot(), source.alive(), source.lineOfSight(), source.legalAttackRange(),
                source.distance(), closingSpeed, source.predictedImpactTicks());
    }

    private static GojosInfinity enabled() {
        GojosInfinity module = new GojosInfinity();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    @SuppressWarnings("unchecked")
    private static dev.helikon.client.setting.EnumSetting<GojosInfinity.RepelMode> enumSetting(
            GojosInfinity module, String id) {
        return (dev.helikon.client.setting.EnumSetting<GojosInfinity.RepelMode>) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(GojosInfinity module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integerSetting(GojosInfinity module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
