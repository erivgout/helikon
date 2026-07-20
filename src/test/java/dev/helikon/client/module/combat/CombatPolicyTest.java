package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.combat.PotionCandidate;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
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
    void hitSelectGatesOnHeldAttackChargeCriticalSprintAndFriends() {
        CombatTarget target = target("target", CombatEntityType.HOSTILE, false, false, true, 3.0D, 0.0D, 10.0D);
        HitSelect hitSelect = enabled(new HitSelect());

        // Held, fully charged, weapon in hand: releases, then respects the delay cadence.
        assertTrue(hitSelect.shouldAttack(0L, target, hitContext(true, 1.0D, false, true, true)));
        assertFalse(hitSelect.shouldAttack(1L, target, hitContext(true, 1.0D, false, true, true)));
        assertTrue(hitSelect.shouldAttack(4L, target, hitContext(true, 1.0D, false, true, true)));

        // Attack not held, below the charge threshold, or missing a weapon: no release.
        assertFalse(hitSelect.shouldAttack(8L, target, hitContext(false, 1.0D, false, true, true)));
        assertFalse(hitSelect.shouldAttack(8L, target, hitContext(true, 0.5D, false, true, true)));
        assertFalse(hitSelect.shouldAttack(8L, target, hitContext(true, 1.0D, false, true, false)));

        // Require critical: a grounded window is rejected, a falling window passes.
        HitSelect criticalOnly = enabled(new HitSelect());
        booleanSetting(criticalOnly, "require_critical").set(true);
        assertFalse(criticalOnly.shouldAttack(0L, target, hitContext(true, 1.0D, false, true, true)));
        assertTrue(criticalOnly.shouldAttack(0L, target, hitContext(true, 1.0D, false, false, true)));

        // Require sprint: only a sprinting hit is allowed.
        HitSelect sprintOnly = enabled(new HitSelect());
        booleanSetting(sprintOnly, "require_sprint").set(true);
        assertFalse(sprintOnly.shouldAttack(0L, target, hitContext(true, 1.0D, false, true, true)));
        assertTrue(sprintOnly.shouldAttack(0L, target, hitContext(true, 1.0D, true, true, true)));

        // Friends are excluded by default.
        CombatTarget friend = target("friend", CombatEntityType.PLAYER, true, false, true, 2.0D, 0.0D, 20.0D);
        assertFalse(enabled(new HitSelect()).shouldAttack(0L, friend, hitContext(true, 1.0D, false, true, true)));
    }

    private static HitSelect.Context hitContext(boolean attackHeld, double charge, boolean sprinting,
                                                boolean onGround, boolean holdingWeapon) {
        double fallDistance = onGround ? 0.0D : 0.5D;
        double verticalVelocity = onGround ? 0.0D : -0.1D;
        return new HitSelect.Context(attackHeld, charge, sprinting, onGround, false, false, false,
                fallDistance, verticalVelocity, holdingWeapon);
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
        assertFalse(aura.settings().stream().anyMatch(setting -> setting.id().equals("rotation_speed")));
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
    void killAuraCanOptionallySelectTargetsWithoutLineOfSight() {
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, false,
                2.0D, 1.0D, 10.0D);
        KillAura normal = enabled(new KillAura());
        assertTrue(normal.nextAttacks(0L, List.of(blocked), true).isEmpty());
        assertFalse(normal.hitThroughWalls());

        KillAura throughWalls = enabled(new KillAura());
        booleanSetting(throughWalls, "hit_through_walls").set(true);
        assertEquals(List.of(blocked), throughWalls.nextAttacks(0L, List.of(blocked), true));
        assertTrue(throughWalls.hitThroughWalls());
    }

    @Test
    void autoClickerFiresOnScheduleAndHonorsHoldScreenAndFriendGates() {
        // Fixed jitter source so the interval is deterministic (min==max keeps it exact regardless).
        AutoClicker clicker = enabled(new AutoClicker(new java.util.Random(1L)));
        numberSetting(clicker, "min_cps").set(10.0D);
        numberSetting(clicker, "max_cps").set(10.0D); // 10 CPS -> 100 ms between clicks
        AutoClicker.Context holdingAir = new AutoClicker.Context(true, false, false, false);

        assertTrue(clicker.shouldClick(0L, holdingAir));       // first eligible moment clicks immediately
        assertFalse(clicker.shouldClick(50L, holdingAir));     // still inside the 100 ms interval
        assertTrue(clicker.shouldClick(100L, holdingAir));     // interval elapsed
        // Releasing the attack button (require_attack_held on) stops clicks and resets the schedule.
        assertFalse(clicker.shouldClick(120L, new AutoClicker.Context(false, false, false, false)));
        assertTrue(clicker.shouldClick(130L, holdingAir));     // re-press clicks immediately again
        // An open screen never clicks.
        assertFalse(clicker.shouldClick(400L, new AutoClicker.Context(true, true, true, false)));
    }

    @Test
    void autoClickerRequiresEntityTargetAndExcludesFriends() {
        AutoClicker clicker = enabled(new AutoClicker(new java.util.Random(1L)));
        boolSetting(clicker, "require_attack_held").set(false);
        boolSetting(clicker, "require_entity_target").set(true);
        AutoClicker.Context friend = new AutoClicker.Context(false, false, true, true);
        AutoClicker.Context foe = new AutoClicker.Context(false, false, true, false);
        AutoClicker.Context air = new AutoClicker.Context(false, false, false, false);

        assertFalse(clicker.shouldAttackEntity(friend)); // excluded friend is never attacked
        assertTrue(clicker.shouldAttackEntity(foe));
        assertFalse(clicker.shouldClick(0L, air));       // require_entity_target: no entity -> no click
        assertFalse(clicker.shouldClick(0L, friend));    // only a friend under crosshair -> no click
        assertTrue(clicker.shouldClick(0L, foe));        // eligible foe -> click
    }

    @Test
    void autoClickerIntervalMathIsBoundedAndRateOrdered() {
        assertEquals(100L, AutoClicker.intervalMillis(10.0D, 10.0D, 0.5D));
        assertEquals(50L, AutoClicker.intervalMillis(20.0D, 20.0D, 0.0D));
        assertEquals(1000L, AutoClicker.intervalMillis(1.0D, 1.0D, 1.0D));
        // Swapped bounds and out-of-range fraction are tolerated.
        assertEquals(AutoClicker.intervalMillis(5.0D, 15.0D, 1.0D), AutoClicker.intervalMillis(15.0D, 5.0D, 1.0D));
        assertTrue(AutoClicker.intervalMillis(5.0D, 15.0D, 2.0D) >= 1L);
    }

    void silentAuraSelectsVisibleTargetsAtBoundedCadenceAndAimsDirectlyWithoutSmoothing() {
        SilentAura aura = enabled(new SilentAura());
        CombatTarget first = target("a", CombatEntityType.HOSTILE, false, false, true, 2.0D, 1.0D, 10.0D);
        CombatTarget second = target("b", CombatEntityType.HOSTILE, false, false, true, 3.0D, 2.0D, 10.0D);
        CombatTarget blocked = target("blocked", CombatEntityType.HOSTILE, false, false, false, 1.0D, 1.0D, 10.0D);

        // Line-of-sight-blocked targets are excluded, and the nearest eligible one is kept in Single mode.
        assertEquals("a", aura.nextAttack(0L, List.of(second, first, blocked), true).orElseThrow().id());
        // The normal attack cooldown suppresses a second attack until the configured delay elapses.
        assertTrue(aura.nextAttack(1L, List.of(second, first, blocked), true).isEmpty());
        assertEquals("a", aura.nextAttack(4L, List.of(second, first, blocked), true).orElseThrow().id());

        enumSetting(aura, "target_mode", SilentAura.TargetMode.class).set(SilentAura.TargetMode.SWITCH);
        assertEquals("b", aura.nextAttack(8L, List.of(second, first, blocked), true).orElseThrow().id());

        // The server-facing aim is the exact rotation toward the target, not a bounded per-tick adjustment,
        // because no visible camera movement is involved.
        CombatTarget straightAhead = new CombatTarget("ahead", "ahead", CombatEntityType.HOSTILE, false, false,
                true, true, true, 4.0D, 0.0D, 0.0D, 0.0D, 4.0D, 0.0D, 0.0D, 0.0D, 20.0D, 0,
                "minecraft:air", List.of());
        CombatAim.Rotation aim = aura.serverAim(straightAhead);
        assertEquals(CombatAim.predictedRotation(straightAhead, 1.0D, 0.0D, false).yaw(), aim.yaw(), 0.0001F);
        assertEquals(0.0F, aim.pitch(), 0.0001F);
    }

    @Test
    void silentAuraRespectsDisabledStateAndMultiModeBounds() {
        SilentAura aura = new SilentAura();
        // A disabled advantage module never selects a target.
        assertTrue(aura.nextAttacks(0L, List.of(target("x", CombatEntityType.HOSTILE, false, false, true,
                2.0D, 1.0D, 10.0D)), true).isEmpty());

        enabled(aura);
        enumSetting(aura, "target_mode", SilentAura.TargetMode.class).set(SilentAura.TargetMode.MULTI);
        numberSetting(aura, "max_targets").set(2.0D);
        CombatTarget nearest = target("nearest", CombatEntityType.HOSTILE, false, false, true, 1.5D, 4.0D, 10.0D);
        CombatTarget middle = target("middle", CombatEntityType.HOSTILE, false, false, true, 2.5D, 2.0D, 10.0D);
        CombatTarget far = target("far", CombatEntityType.HOSTILE, false, false, true, 3.5D, 1.0D, 10.0D);

        assertEquals(List.of(nearest, middle), aura.nextAttacks(0L, List.of(far, middle, nearest), true));
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
    void rightClickerRespectsRateTargetFiltersFriendsAndGates() {
        RightClicker rightClicker = enabled(new RightClicker());
        booleanSetting(rightClicker, "require_use_key_held").set(false);
        // At the default 8 clicks per second the interval is round(20/8) = 3 ticks.
        RightClicker.Context block = context(RightClicker.HitKind.BLOCK, false, true);
        assertEquals(RightClicker.Decision.USE_ON_BLOCK, rightClicker.decide(0L, block));
        assertEquals(RightClicker.Decision.NONE, rightClicker.decide(2L, block));
        assertEquals(RightClicker.Decision.USE_ON_BLOCK, rightClicker.decide(3L, block));

        // A blank target for the current settings does not consume the cooldown.
        booleanSetting(rightClicker, "blocks").set(false);
        assertEquals(RightClicker.Decision.NONE, rightClicker.decide(6L, block));
        booleanSetting(rightClicker, "blocks").set(true);
        assertEquals(RightClicker.Decision.USE_ON_BLOCK, rightClicker.decide(6L, block));

        // Friends are excluded by default while entity interaction is allowed.
        assertEquals(RightClicker.Decision.INTERACT_ENTITY,
                rightClicker.decide(9L, context(RightClicker.HitKind.ENTITY, false, false)));
        assertEquals(RightClicker.Decision.NONE,
                rightClicker.decide(12L, context(RightClicker.HitKind.ENTITY, true, false)));

        // Air use requires a held item; screens and in-progress use always block.
        assertEquals(RightClicker.Decision.USE_ITEM,
                rightClicker.decide(15L, context(RightClicker.HitKind.MISS, false, true)));
        assertEquals(RightClicker.Decision.NONE,
                rightClicker.decide(18L, context(RightClicker.HitKind.MISS, false, false)));
        assertEquals(RightClicker.Decision.NONE,
                rightClicker.decide(21L, new RightClicker.Context(true, true, false, false, true,
                        RightClicker.HitKind.BLOCK, false)));
        assertEquals(RightClicker.Decision.NONE,
                rightClicker.decide(24L, new RightClicker.Context(true, false, false, true, true,
                        RightClicker.HitKind.BLOCK, false)));

        // The require-use-key gate rejects clicks while the physical button is up.
        booleanSetting(rightClicker, "require_use_key_held").set(true);
        assertEquals(RightClicker.Decision.NONE,
                rightClicker.decide(27L, new RightClicker.Context(true, false, false, false, true,
                        RightClicker.HitKind.BLOCK, false)));

        // The configured click rate maps to a bounded per-tick interval (default 8 CPS -> 3 ticks).
        RightClicker fresh = new RightClicker();
        assertEquals(3L, fresh.intervalTicks());
        numberSetting(fresh, "clicks_per_second").set(20.0D);
        assertEquals(1L, fresh.intervalTicks());
    }

    private static RightClicker.Context context(RightClicker.HitKind kind, boolean friend, boolean hasHeldItem) {
        return new RightClicker.Context(true, false, true, false, hasHeldItem, kind, friend);
    }

    @Test
    void autoPearlThrowsAtFleeingTargetThenRestoresOwnedSlot() {
        AutoPearl pearl = enabled(new AutoPearl());
        CombatTarget fleeing = fleeing("run", 20.0D, 0.5D);

        AutoPearl.Action thrown = pearl.update(0L, new AutoPearl.Context(4, 2, false, false, List.of(fleeing)));
        assertEquals(AutoPearl.ActionType.SELECT_AND_THROW, thrown.type());
        assertEquals(2, thrown.slot());
        assertTrue(thrown.rotate());
        assertTrue(thrown.pitch() < 0.0F, "an away-and-level target requires a slight upward pearl arc");

        AutoPearl.Action restore = pearl.update(1L, new AutoPearl.Context(2, 2, false, false, List.of()));
        assertEquals(AutoPearl.ActionType.RESTORE_SLOT, restore.type());
        assertEquals(4, restore.slot());
    }

    @Test
    void autoPearlSkipsApproachingNearCoolingDownAndFriendTargets() {
        AutoPearl pearl = enabled(new AutoPearl());
        CombatTarget approaching = fleeing("toward", 20.0D, -0.5D);
        CombatTarget tooNear = fleeing("near", 5.0D, 0.5D);
        CombatTarget fleeing = fleeing("run", 20.0D, 0.5D);
        CombatTarget friend = new CombatTarget("friend", "friend", CombatEntityType.PLAYER, true, false, true, true,
                false, 20.0D, 0.0D, 0.0D, 0.0D, 20.0D, 0.0D, 0.0D, 0.5D, 20.0D, 0, "minecraft:air", List.of());

        assertEquals(AutoPearl.ActionType.NONE,
                pearl.update(0L, new AutoPearl.Context(4, 2, false, false, List.of(approaching))).type());
        assertEquals(AutoPearl.ActionType.NONE,
                pearl.update(0L, new AutoPearl.Context(4, 2, false, false, List.of(tooNear))).type());
        assertEquals(AutoPearl.ActionType.NONE,
                pearl.update(0L, new AutoPearl.Context(4, 2, false, false, List.of(friend))).type());
        assertEquals(AutoPearl.ActionType.NONE,
                pearl.update(0L, new AutoPearl.Context(4, 2, false, true, List.of(fleeing))).type());
        assertEquals(AutoPearl.ActionType.NONE,
                pearl.update(0L, new AutoPearl.Context(4, -1, false, false, List.of(fleeing))).type());
    }

    private static CombatTarget fleeing(String id, double distance, double awayVelocityZ) {
        return new CombatTarget(id, id, CombatEntityType.PLAYER, false, false, true, true, false, distance, 0.0D,
                0.0D, 0.0D, distance, 0.0D, 0.0D, awayVelocityZ, 20.0D, 0, "minecraft:air", List.of());
    }

    @Test
    void autoSoupUsesAnOwnedHotbarStewAndRestoresThePriorSlot() {
        AutoSoup autoSoup = enabled(new AutoSoup());
        AutoSoup.Action use = autoSoup.update(0L, new AutoSoup.Context(1, 5.0D, false, false, List.of(4, 3)));
        assertEquals(AutoSoup.ActionType.SELECT_AND_USE, use.type());
        assertEquals(3, use.slot());
        AutoSoup.Action restore = autoSoup.update(1L, new AutoSoup.Context(3, 5.0D, false, false, List.of()));
        assertEquals(AutoSoup.ActionType.RESTORE_SLOT, restore.type());
        assertEquals(1, restore.slot());
    }

    @Test
    void clickAuraRequiresHeldAttackAndChoosesTheNearestEligibleTarget() {
        ClickAura clickAura = enabled(new ClickAura());
        CombatTarget near = target("near", CombatEntityType.HOSTILE, false, false, true, 2.0D, 30.0D, 10.0D);
        CombatTarget friend = target("friend", CombatEntityType.PLAYER, true, false, true, 1.0D, 0.0D, 10.0D);
        assertEquals("near", clickAura.nextAttack(0L, List.of(friend, near), true, true).orElseThrow().id());
        assertTrue(clickAura.nextAttack(1L, List.of(friend, near), true, true).isEmpty());
        assertTrue(clickAura.nextAttack(8L, List.of(near), false, true).isEmpty());
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

    private static BooleanSetting boolSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
