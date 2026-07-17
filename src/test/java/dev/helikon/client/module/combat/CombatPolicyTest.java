package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.combat.PotionCandidate;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatPolicyTest {
    @Test
    void targetFilterExcludesFriendsBotsAndSolidObstructionsBeforeDeterministicPriority() {
        CombatTarget nearFriend = target("friend", CombatEntityType.PLAYER, true, false, true, 2.0D, 5.0D, 20.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, false, 1.0D, 2.0D, 8.0D);
        CombatTarget safe = target("safe", CombatEntityType.HOSTILE, false, false, true, 3.0D, 10.0D, 4.0D);
        CombatTarget bot = target("bot", CombatEntityType.PLAYER, false, true, true, 1.0D, 2.0D, 2.0D);
        CombatTargetFilter.Options options = new CombatTargetFilter.Options(true, true, false, true, true,
                6.0D, 90.0D, true);

        assertEquals(List.of(safe), CombatTargetFilter.ordered(List.of(nearFriend, blocked, safe, bot), options,
                CombatTargetFilter.Priority.DISTANCE));
        CombatTarget invisibleButAllowedByAntiBot = new CombatTarget("invisible", "invisible", CombatEntityType.PLAYER,
                false, false, true, false, true, 2.0D, 0.0D, 0.0D, 1.0D, 2.0D,
                0.0D, 0.0D, 0.0D, 10.0D, 0, "minecraft:air", List.of());
        assertTrue(CombatTargetFilter.allows(invisibleButAllowedByAntiBot, options));
    }

    @Test
    void bowAimPredictsAndBoundsLocalRotationChange() {
        CombatTarget target = new CombatTarget("target", "target", CombatEntityType.HOSTILE, false, false,
                true, true, true, 12.0D, 0.0D, 12.0D, 1.0D, 12.0D, 0.1D, 0.0D, 0.0D,
                20.0D, 0, "minecraft:air", List.of());
        CombatAim.Rotation desired = CombatAim.predictedRotation(target, 3.0D, 0.05D, true);
        CombatAim.Rotation limited = CombatAim.limit(new CombatAim.Rotation(0.0F, 0.0F), desired, 4.0D);

        assertTrue(desired.pitch() < 0.0F);
        assertEquals(4.0F, Math.abs(limited.yaw()), 0.0001F);
        assertTrue(Math.abs(limited.pitch()) <= 4.0F);
    }

    @Test
    void triggerBotAndCriticalAssistRequireNormalCooldownAndLegitimateConditions() {
        CombatTarget target = target("target", CombatEntityType.HOSTILE, false, false, true, 3.0D, 0.0D, 10.0D);
        TriggerBot triggerBot = enabled(new TriggerBot());
        assertTrue(triggerBot.shouldAttack(0L, target, true, true));
        assertFalse(triggerBot.shouldAttack(1L, target, true, true));
        assertTrue(triggerBot.shouldAttack(4L, target, true, true));
        assertFalse(triggerBot.shouldAttack(8L, target, true, false));

        CriticalAssist critical = enabled(new CriticalAssist());
        CriticalAssist.Context falling = new CriticalAssist.Context(true, true, false, false, false, false, 0.5D, -0.1D);
        assertTrue(critical.shouldAttack(0L, target, falling));
        assertFalse(critical.shouldAttack(4L, target,
                new CriticalAssist.Context(true, true, true, false, false, false, 0.5D, -0.1D)));
    }

    @Test
    void killAuraKeepsOrSwitchesOnlyVisibleEligibleTargets() {
        KillAura aura = enabled(new KillAura());
        CombatTarget first = target("a", CombatEntityType.HOSTILE, false, false, true, 2.0D, 1.0D, 10.0D);
        CombatTarget second = target("b", CombatEntityType.HOSTILE, false, false, true, 3.0D, 2.0D, 10.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, false, 1.0D, 1.0D, 10.0D);
        assertEquals("a", aura.nextAttack(0L, List.of(second, first, blocked), true).orElseThrow().id());
        assertEquals("a", aura.nextAttack(4L, List.of(second, first, blocked), true).orElseThrow().id());

        enumSetting(aura, "target_mode", KillAura.TargetMode.class).set(KillAura.TargetMode.SWITCH);
        assertEquals("b", aura.nextAttack(8L, List.of(second, first, blocked), true).orElseThrow().id());
        CombatAim.Rotation rotation = aura.rotateToward(second, new CombatAim.Rotation(0.0F, 0.0F));
        assertTrue(Math.abs(rotation.yaw()) <= 8.0F);
        assertTrue(Math.abs(rotation.pitch()) <= 8.0F);
    }

    @Test
    void killAuraMultiModeReturnsBoundedPriorityOrderedTargets() {
        KillAura aura = enabled(new KillAura());
        enumSetting(aura, "target_mode", KillAura.TargetMode.class).set(KillAura.TargetMode.MULTI);
        numberSetting(aura, "max_targets").set(2.0D);
        CombatTarget nearest = target("nearest", CombatEntityType.HOSTILE, false, false, true, 1.5D, 4.0D, 10.0D);
        CombatTarget second = target("second", CombatEntityType.HOSTILE, false, false, true, 2.5D, 2.0D, 10.0D);
        CombatTarget third = target("third", CombatEntityType.HOSTILE, false, false, true, 3.5D, 1.0D, 10.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, false, 1.0D, 1.0D, 10.0D);

        assertEquals(List.of(nearest, second),
                aura.nextAttacks(0L, List.of(third, blocked, second, nearest), true));
        assertTrue(aura.nextAttacks(1L, List.of(nearest, second, third), true).isEmpty());
    }

    @Test
    void autoPotionUsesOnlyRestorativeWhitelistedPotionAndRestoresOwnedSlot() {
        AutoPotion autoPotion = enabled(new AutoPotion());
        PotionCandidate safe = new PotionCandidate(3, "healing", PotionCandidate.Kind.SPLASH, true);
        PotionCandidate unsafe = new PotionCandidate(4, "harming", PotionCandidate.Kind.SPLASH, false);
        AutoPotion.Action use = autoPotion.update(0L, new AutoPotion.Context(1, 5.0D, false, false, List.of(unsafe, safe)));
        assertEquals(AutoPotion.ActionType.SELECT_AND_USE, use.type());
        assertEquals(3, use.slot());
        AutoPotion.Action restore = autoPotion.update(1L, new AutoPotion.Context(3, 5.0D, false, false, List.of()));
        assertEquals(AutoPotion.ActionType.RESTORE_SLOT, restore.type());
        assertEquals(1, restore.slot());
    }

    @Test
    void antiBotAndHudTrackerRemainLocalAndBounded() {
        AntiBot antiBot = enabled(new AntiBot());
        assertTrue(antiBot.isSuspected(new AntiBot.Facts(false, false, 20, false, false, true)));
        assertFalse(antiBot.isSuspected(new AntiBot.Facts(true, false, 20, false, false, true)));

        CombatTargetTracker tracker = new CombatTargetTracker();
        CombatTarget target = target("target", CombatEntityType.HOSTILE, false, false, true, 2.5D, 0.0D, 8.0D);
        tracker.recordAttack(target);
        assertEquals("target", tracker.target().orElseThrow().id());
        assertEquals(2.5D, tracker.lastAttackDistance().orElseThrow(), 0.0001D);
        tracker.clearIfAbsent(List.of(target));
        assertTrue(tracker.target().isPresent());
        tracker.clearIfAbsent(List.of());
        assertTrue(tracker.target().isEmpty());
        tracker.clear();
        assertTrue(tracker.target().isEmpty());
        assertTrue(tracker.lastAttackDistance().isEmpty());
    }

    @Test
    void blockHitRaisesShieldForInRangeThreatAndUnblocksAroundReadyAttack() {
        FakeUseKey key = new FakeUseKey();
        BlockHit blockHit = enabled(new BlockHit(key));
        CombatTarget threat = target("threat", CombatEntityType.HOSTILE, false, false, false, 3.0D, 0.0D, 20.0D);

        BlockHit.Action idle = blockHit.tick(0L, new BlockHit.Context(true, true, false, false, List.of()));
        assertFalse(idle.holdBlock());
        assertFalse(key.down);

        BlockHit.Action noShield = blockHit.tick(1L, new BlockHit.Context(false, true, false, false, List.of(threat)));
        assertFalse(noShield.holdBlock());
        assertFalse(key.down);

        BlockHit.Action raise = blockHit.tick(2L, new BlockHit.Context(true, true, false, false, List.of(threat)));
        assertTrue(raise.holdBlock());
        assertTrue(key.down);

        BlockHit.Action unblock = blockHit.tick(3L, new BlockHit.Context(true, true, true, false, List.of(threat)));
        assertTrue(unblock.releaseBlock());
        assertFalse(key.down);

        BlockHit.Action reblock = blockHit.tick(6L, new BlockHit.Context(true, true, false, false, List.of(threat)));
        assertTrue(reblock.holdBlock());
        assertTrue(key.down);

        blockHit.disable();
        assertFalse(key.down);
    }

    private static final class FakeUseKey implements BlockHit.UseKeyAccess {
        private boolean physicallyDown;
        private boolean down;

        @Override
        public boolean isPhysicallyDown() {
            return physicallyDown;
        }

        @Override
        public void setDown(boolean value) {
            down = value;
        }
    }

    private static CombatTarget target(String id, CombatEntityType type, boolean friend, boolean bot, boolean lineOfSight,
                                       double distance, double angle, double health) {
        return new CombatTarget(id, id, type, friend, bot, true, true, lineOfSight, distance, angle,
                0.0D, 1.0D, distance, 0.1D, 0.0D, 0.0D, health, 0, "minecraft:air", List.of());
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> EnumSetting<E> enumSetting(Module module, String id, Class<E> ignored) {
        return (EnumSetting<E>) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Module module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
