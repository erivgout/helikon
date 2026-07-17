package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
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

class GojosInfinityTest {
    @Test
    void identityAndDefaultsAreDefensiveAndFriendSafe() {
        GojosInfinity module = new GojosInfinity();

        assertEquals("gojo_infinity", module.id());
        assertEquals("Gojo's Infinity", module.name());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.excludeFriends());
        assertTrue(module.selectThreat(0L, true, List.of(threat("x", 3.0D, 0.2D))).isEmpty());
    }

    @Test
    void selectsNearestClosingVisibleEligibleThreat() {
        GojosInfinity module = enabledModule();
        GojosInfinity.Threat far = threat("far", 4.0D, 0.2D);
        GojosInfinity.Threat near = threat("near", 2.5D, 0.1D);
        GojosInfinity.Threat retreating = threat("away", 2.0D, -0.2D);
        GojosInfinity.Threat blocked = new GojosInfinity.Threat("blocked", CombatEntityType.PLAYER,
                false, false, true, false, 1.5D, 0.2D);

        assertEquals("near", module.selectThreat(0L, true,
                List.of(far, retreating, blocked, near)).orElseThrow());
    }

    @Test
    void excludesFriendsBotsDeadTargetsAndDisabledTypes() {
        GojosInfinity module = enabledModule();
        GojosInfinity.Threat friend = new GojosInfinity.Threat("friend", CombatEntityType.PLAYER,
                true, false, true, true, 2.0D, 0.2D);
        GojosInfinity.Threat bot = new GojosInfinity.Threat("bot", CombatEntityType.PLAYER,
                false, true, true, true, 2.0D, 0.2D);
        GojosInfinity.Threat dead = new GojosInfinity.Threat("dead", CombatEntityType.HOSTILE,
                false, false, false, true, 2.0D, 0.2D);
        GojosInfinity.Threat passive = new GojosInfinity.Threat("passive", CombatEntityType.PASSIVE,
                false, false, true, true, 2.0D, 0.2D);

        assertTrue(module.selectThreat(0L, true, List.of(friend, bot, dead, passive)).isEmpty());
        booleanSetting(module, "exclude_friends").set(false);
        assertEquals("friend", module.selectThreat(0L, true, List.of(friend)).orElseThrow());
    }

    @Test
    void readinessApproachRangeAndCadenceGateActions() {
        GojosInfinity module = enabledModule();
        integerSetting(module, "delay_ticks").set(4);
        GojosInfinity.Threat valid = threat("valid", 3.0D, 0.2D);

        assertTrue(module.selectThreat(0L, false, List.of(valid)).isEmpty());
        assertEquals("valid", module.selectThreat(0L, true, List.of(valid)).orElseThrow());
        assertTrue(module.selectThreat(3L, true, List.of(valid)).isEmpty());
        assertEquals("valid", module.selectThreat(4L, true, List.of(valid)).orElseThrow());

        module.resetTransientState();
        assertEquals("valid", module.selectThreat(0L, true, List.of(valid)).orElseThrow());

        module.disable();
        module.enable();
        assertTrue(module.selectThreat(0L, true, List.of(threat("far", 5.0D, 0.2D))).isEmpty());
        assertTrue(module.selectThreat(0L, true, List.of(threat("slow", 3.0D, 0.01D))).isEmpty());
    }

    @Test
    void approachAndAttackReadinessRequirementsCanBeDisabled() {
        GojosInfinity module = enabledModule();
        booleanSetting(module, "require_approaching").set(false);
        booleanSetting(module, "require_attack_ready").set(false);

        assertEquals("still", module.selectThreat(0L, false,
                List.of(threat("still", 3.0D, 0.0D))).orElseThrow());
    }

    @Test
    void settingAndFactBoundsAreValidated() {
        GojosInfinity module = new GojosInfinity();
        NumberSetting radius = numberSetting(module, "barrier_radius");
        NumberSetting attackDistance = numberSetting(module, "attack_distance");
        IntegerSetting delay = integerSetting(module, "delay_ticks");

        assertEquals(2.0D, radius.minimum());
        assertEquals(8.0D, radius.maximum());
        assertEquals(1.0D, attackDistance.minimum());
        assertEquals(3.0D, attackDistance.maximum());
        assertEquals(1, delay.minimum());
        assertEquals(40, delay.maximum());
        assertThrows(IllegalArgumentException.class, () -> radius.set(8.1D));
        assertThrows(IllegalArgumentException.class,
                () -> new GojosInfinity.Threat("", CombatEntityType.PLAYER,
                        false, false, true, true, 1.0D, 0.1D));
    }

    private static GojosInfinity.Threat threat(String id, double distance, double closingSpeed) {
        return new GojosInfinity.Threat(id, CombatEntityType.PLAYER,
                false, false, true, true, distance, closingSpeed);
    }

    private static GojosInfinity enabledModule() {
        GojosInfinity module = new GojosInfinity();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(GojosInfinity module, String id) {
        return (BooleanSetting) module.settings().stream()
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
