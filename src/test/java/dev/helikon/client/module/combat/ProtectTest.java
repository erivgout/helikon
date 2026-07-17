package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectTest {
    @Test
    void identityDefaultsAndDisabledStateAreSafe() {
        Protect module = new Protect();

        assertEquals("protect", module.id());
        assertEquals("Protect", module.name());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertTrue(module.update(0L, context(true, List.of(friend("Alex", 3.0D, 0.0D)))).isEmpty());
    }

    @Test
    void followsConfiguredFriendAtBoundedSpeed() {
        Protect module = enabledModule();
        stringSetting(module, "protected_friend").set("Alex");
        Protect.Action action = module.update(0L, context(false, List.of(
                friend("Steve", 4.0D, 0.0D),
                friend("Alex", 0.0D, 10.0D)
        ))).orElseThrow();

        assertEquals("Alex", action.protectedFriend().name());
        assertTrue(action.move());
        assertEquals(0.0D, action.velocityX(), 0.0001D);
        assertEquals(0.18D, action.velocityZ(), 0.0001D);
        assertTrue(action.threat().isEmpty());
        assertFalse(action.attack());
    }

    @Test
    void attacksNearestThreatToProtectedFriendAtCadence() {
        Protect module = enabledModule();
        integerSetting(module, "delay_ticks").set(4);
        CombatTarget friend = friend("Alex", 2.5D, 0.0D);
        CombatTarget nearFriend = target("near", CombatEntityType.HOSTILE,
                false, false, true, true, 2.0D, 0.0D);
        CombatTarget fartherFromFriend = target("far", CombatEntityType.HOSTILE,
                false, false, true, true, 0.0D, 2.0D);

        Protect.Action first = module.update(0L,
                context(true, List.of(friend, fartherFromFriend, nearFriend))).orElseThrow();
        assertEquals("near", first.threat().orElseThrow().id());
        assertTrue(first.attack());
        assertFalse(module.update(3L, context(true, List.of(friend, nearFriend))).orElseThrow().attack());
        assertTrue(module.update(4L, context(true, List.of(friend, nearFriend))).orElseThrow().attack());

        module.resetTransientState();
        assertTrue(module.update(0L, context(true, List.of(friend, nearFriend))).orElseThrow().attack());
    }

    @Test
    void neverTreatsFriendsBotsDeadBlockedOrPassiveEntitiesAsThreats() {
        Protect module = enabledModule();
        CombatTarget protectedFriend = friend("Alex", 2.0D, 0.0D);
        CombatTarget otherFriend = friend("Steve", 2.2D, 0.0D);
        CombatTarget bot = target("bot", CombatEntityType.PLAYER,
                false, true, true, true, 2.2D, 0.0D);
        CombatTarget dead = target("dead", CombatEntityType.HOSTILE,
                false, false, false, true, 2.2D, 0.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE,
                false, false, true, false, 2.2D, 0.0D);
        CombatTarget passive = target("passive", CombatEntityType.PASSIVE,
                false, false, true, true, 2.2D, 0.0D);

        Protect.Action action = module.update(0L,
                context(true, List.of(protectedFriend, otherFriend, bot, dead, blocked, passive))).orElseThrow();
        assertTrue(action.threat().isEmpty());
        assertFalse(action.attack());
    }

    @Test
    void protectionAndAttackRangesAreIndependent() {
        Protect module = enabledModule();
        CombatTarget friend = friend("Alex", 5.0D, 0.0D);
        CombatTarget closeToFriendButFarFromPlayer = target("threat", CombatEntityType.HOSTILE,
                false, false, true, true, 6.0D, 0.0D);

        Protect.Action action = module.update(0L,
                context(true, List.of(friend, closeToFriendButFarFromPlayer))).orElseThrow();
        assertEquals("threat", action.threat().orElseThrow().id());
        assertFalse(action.attack());
        numberSetting(module, "protection_radius").set(2.0D);
        CombatTarget outsideProtection = target("outside", CombatEntityType.HOSTILE,
                false, false, true, true, 8.0D, 0.0D);
        assertTrue(module.update(1L, context(true, List.of(friend, outsideProtection)))
                .orElseThrow().threat().isEmpty());
    }

    @Test
    void unsafeContextsAndSettingsValidate() {
        Protect module = enabledModule();
        CombatTarget friend = friend("Alex", 3.0D, 0.0D);
        assertTrue(module.update(0L, new Protect.Context(true, false, false, false,
                true, List.of(friend))).isEmpty());
        assertTrue(module.update(0L, new Protect.Context(false, true, false, false,
                true, List.of(friend))).isEmpty());
        assertTrue(module.update(0L, new Protect.Context(false, false, true, false,
                true, List.of(friend))).isEmpty());
        assertTrue(module.update(0L, new Protect.Context(false, false, false, true,
                true, List.of(friend))).isEmpty());

        NumberSetting followRange = numberSetting(module, "follow_range");
        IntegerSetting delay = integerSetting(module, "delay_ticks");
        assertEquals(2.0D, followRange.minimum());
        assertEquals(64.0D, followRange.maximum());
        assertEquals(2, delay.minimum());
        assertEquals(40, delay.maximum());
        assertThrows(IllegalArgumentException.class, () -> followRange.set(64.1D));
        assertThrows(IllegalArgumentException.class,
                () -> new Protect.Action(friend, java.util.Optional.empty(),
                        false, 0.1D, 0.0D, false));
    }

    private static Protect.Context context(boolean attackReady, List<CombatTarget> targets) {
        return new Protect.Context(false, false, false, false, attackReady, targets);
    }

    private static CombatTarget friend(String name, double x, double z) {
        return target(name, CombatEntityType.PLAYER, true, false, true, true, x, z);
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend, boolean bot,
                                       boolean alive, boolean lineOfSight, double x, double z) {
        double distance = Math.hypot(x, z);
        return new CombatTarget(id, id, type, friend, bot, alive, true, lineOfSight, distance, 0.0D,
                x, 0.0D, z, 0.0D, 0.0D, 0.0D, 20.0D, 0, "minecraft:stone", List.of());
    }

    private static Protect enabledModule() {
        Protect module = new Protect();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static StringSetting stringSetting(Protect module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Protect module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integerSetting(Protect module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
