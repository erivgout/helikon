package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
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

class FightBotTest {
    @Test
    void identityAndDefaultsAreSafe() {
        FightBot module = new FightBot();

        assertEquals("fight_bot", module.id());
        assertEquals("FightBot", module.name());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.excludeFriends());
        assertTrue(module.update(0L, context(true, List.of(target("enemy", 2.0D, 2.0D, 0.0D)))).isEmpty());
    }

    @Test
    void approachesNearestEligibleEnemyAtBoundedSpeed() {
        FightBot module = enabledModule();
        FightBot.Action action = module.update(0L, context(false, List.of(
                target("far", 10.0D, 10.0D, 0.0D),
                target("near", 5.0D, 0.0D, 5.0D)
        ))).orElseThrow();

        assertEquals("near", action.target().id());
        assertTrue(action.move());
        assertEquals(0.0D, action.velocityX(), 0.0001D);
        assertEquals(0.18D, action.velocityZ(), 0.0001D);
        assertFalse(action.attack());
    }

    @Test
    void stopsAndAttacksWithinRangeAtConfiguredCadence() {
        FightBot module = enabledModule();
        integerSetting(module, "delay_ticks").set(4);
        CombatTarget close = target("close", 2.0D, 2.0D, 0.0D);

        FightBot.Action first = module.update(0L, context(true, List.of(close))).orElseThrow();
        assertFalse(first.move());
        assertTrue(first.attack());
        assertFalse(module.update(3L, context(true, List.of(close))).orElseThrow().attack());
        assertTrue(module.update(4L, context(true, List.of(close))).orElseThrow().attack());

        module.resetTransientState();
        assertTrue(module.update(0L, context(true, List.of(close))).orElseThrow().attack());
    }

    @Test
    void excludesFriendsBotsBlockedDeadAndPassiveTargets() {
        FightBot module = enabledModule();
        CombatTarget friend = target("friend", CombatEntityType.PLAYER, true, false, true, true, 2.0D);
        CombatTarget bot = target("bot", CombatEntityType.PLAYER, false, true, true, true, 2.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, true, false, 2.0D);
        CombatTarget dead = target("dead", CombatEntityType.HOSTILE, false, false, false, true, 2.0D);
        CombatTarget passive = target("passive", CombatEntityType.PASSIVE, false, false, true, true, 2.0D);

        assertTrue(module.update(0L, context(true, List.of(friend, bot, blocked, dead, passive))).isEmpty());
        booleanSetting(module, "exclude_friends").set(false);
        assertEquals("friend", module.update(0L, context(true, List.of(friend))).orElseThrow().target().id());
    }

    @Test
    void unsafeContextsAndSearchRangePreventAction() {
        FightBot module = enabledModule();
        CombatTarget target = target("enemy", 5.0D, 5.0D, 0.0D);

        assertTrue(module.update(0L, new FightBot.Context(true, false, false, false,
                true, List.of(target))).isEmpty());
        assertTrue(module.update(0L, new FightBot.Context(false, true, false, false,
                true, List.of(target))).isEmpty());
        assertTrue(module.update(0L, new FightBot.Context(false, false, true, false,
                true, List.of(target))).isEmpty());
        assertTrue(module.update(0L, new FightBot.Context(false, false, false, true,
                true, List.of(target))).isEmpty());
        numberSetting(module, "search_range").set(3.0D);
        assertTrue(module.update(0L, context(true, List.of(target))).isEmpty());
    }

    @Test
    void settingsAndActionsValidateBounds() {
        FightBot module = new FightBot();
        NumberSetting searchRange = numberSetting(module, "search_range");
        NumberSetting movementSpeed = numberSetting(module, "movement_speed");
        IntegerSetting delay = integerSetting(module, "delay_ticks");

        assertEquals(3.0D, searchRange.minimum());
        assertEquals(32.0D, searchRange.maximum());
        assertEquals(0.05D, movementSpeed.minimum());
        assertEquals(0.30D, movementSpeed.maximum());
        assertEquals(2, delay.minimum());
        assertEquals(40, delay.maximum());
        assertThrows(IllegalArgumentException.class, () -> searchRange.set(32.1D));
        assertThrows(IllegalArgumentException.class,
                () -> new FightBot.Action(target("enemy", 2.0D, 2.0D, 0.0D),
                        false, 0.1D, 0.0D, false));
    }

    private static FightBot.Context context(boolean attackReady, List<CombatTarget> targets) {
        return new FightBot.Context(false, false, false, false, attackReady, targets);
    }

    private static CombatTarget target(String id, double distance, double x, double z) {
        return target(id, CombatEntityType.PLAYER, false, false, true, true, distance, x, z);
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend, boolean bot,
                                       boolean alive, boolean lineOfSight, double distance) {
        return target(id, type, friend, bot, alive, lineOfSight, distance, distance, 0.0D);
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend, boolean bot,
                                       boolean alive, boolean lineOfSight, double distance, double x, double z) {
        return new CombatTarget(id, id, type, friend, bot, alive, true, lineOfSight, distance, 0.0D,
                x, 0.0D, z, 0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:stone", List.of());
    }

    private static FightBot enabledModule() {
        FightBot module = new FightBot();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(FightBot module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(FightBot module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integerSetting(FightBot module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
