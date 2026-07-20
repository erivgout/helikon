package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainExpansionTest {
    @Test
    void identityDefaultsAndStateCatalogMatchCombatModule() {
        DomainExpansion module = new DomainExpansion();

        assertEquals("domain_expansion", module.id());
        assertEquals("Domain Expansion", module.name());
        assertEquals(ModuleCategory.COMBAT, module.category());
        assertFalse(module.defaultEnabled());
        assertEquals(DomainExpansion.State.IDLE, module.state());
        assertEquals(12, DomainExpansion.State.values().length);
        assertEquals(List.of(
                "minecraft:obsidian",
                "minecraft:crying_obsidian",
                "minecraft:ender_chest",
                "minecraft:cobblestone",
                "*"
        ), module.allowedBlocks());
        assertTrue(bool(module, "players").value());
        assertTrue(bool(module, "hostiles").value());
        assertFalse(bool(module, "passive").value());
        assertTrue(bool(module, "exclude_friends").value());
    }

    @Test
    void calculatesAdjacentSeveralBlocksAndDiagonalPlayerBounds() {
        DomainBounds adjacent = DomainBoundsCalculator.calculate(
                pos(10, 64, 10), pos(11, 64, 10), 1, 3, 10, 10).orElseThrow();
        assertEquals(6, adjacent.width());
        assertEquals(5, adjacent.length());
        assertTrue(adjacent.containsFeet(pos(10, 64, 10)));
        assertTrue(adjacent.containsFeet(pos(11, 64, 10)));

        DomainBounds apart = DomainBoundsCalculator.calculate(
                pos(10, 64, 10), pos(14, 64, 10), 1, 3, 10, 10).orElseThrow();
        assertEquals(9, apart.width());
        assertEquals(5, apart.length());

        DomainBounds diagonal = DomainBoundsCalculator.calculate(
                pos(10, 64, 10), pos(13, 64, 12), 1, 3, 10, 10).orElseThrow();
        assertEquals(8, diagonal.width());
        assertEquals(7, diagonal.length());
        assertTrue(diagonal.containsFeet(pos(10, 64, 10)));
        assertTrue(diagonal.containsFeet(pos(13, 64, 12)));
    }

    @Test
    void rejectsVerticalOrHorizontalDimensionsThatCannotFit() {
        assertTrue(DomainBoundsCalculator.calculate(
                pos(0, 64, 0), pos(8, 64, 0), 1, 3, 8, 10).isEmpty());
        assertTrue(DomainBoundsCalculator.calculate(
                pos(0, 64, 0), pos(1, 68, 0), 1, 3, 10, 10).isEmpty());

        DomainExpansion module = enabledManual();
        integer(module, "maximum_width").set(5);
        DomainExpansion.TickResult result = module.tick(context(0, pos(0, 64, 0),
                List.of(target("far", pos(3, 64, 0), 3.0D, false)), 512, new FakeWorld()));
        assertEquals(DomainExpansion.CancelReason.DIMENSIONS_EXCEEDED, result.cancelReason());
        assertEquals(DomainExpansion.State.CANCELLED, module.state());
    }

    @Test
    void escapeDirectionPrioritizesFrontThenAdjacentWalls() {
        DomainBounds bounds = DomainBoundsCalculator.calculate(
                pos(0, 64, 0), pos(2, 64, 0), 1, 3, 10, 10).orElseThrow();
        DomainTarget eastbound = new DomainTarget("target", "target", CombatEntityType.PLAYER, pos(2, 64, 0),
                2.5D, 0.5D, 2.0D, 20.0D, 0.0D,
                0.3D, 0.0D, 1.0D, 0.0D,
                false, true, false, false, true, false);
        DomainPlacementPlan plan = DomainPlanGenerator.generate(bounds, pos(0, 64, 0), eastbound,
                true, false, DomainPlanGenerator.ExitMode.NO_EXIT, true);

        DomainPosition first = plan.entries(DomainPlacementPlan.Part.WALL).getFirst().position();
        assertEquals(bounds.wallMaxX(), first.x());
        assertEquals(bounds.floorY(), first.y());
    }

    @Test
    void existingTerrainCountsTowardCompletionWithoutReplacement() {
        DomainExpansion module = enabledManual();
        FakeWorld world = new FakeWorld();
        DomainPosition local = pos(0, 64, 0);
        DomainTarget target = target("enemy", pos(1, 64, 0), 1.0D, false);
        DomainBounds bounds = DomainBoundsCalculator.calculate(local, target.feet(), 1, 3, 10, 10).orElseThrow();
        DomainPlacementPlan plan = DomainPlanGenerator.generate(bounds, local, target, true, false,
                DomainPlanGenerator.ExitMode.NO_EXIT, true);
        plan.entries(DomainPlacementPlan.Part.WALL).stream()
                .filter(entry -> entry.position().z() == bounds.wallMinZ())
                .forEach(entry -> world.solid.add(entry.position()));

        runToTerminal(module, local, target, world, true);

        assertEquals(DomainExpansion.State.COMPLETE, module.state());
        assertEquals(1.0D, module.completion());
        assertTrue(module.attemptedPlacements() < plan.requiredBlocks());
    }

    @Test
    void missingRoofSupportIsRetriedThenFailsWithoutAirPlacement() {
        DomainExpansion module = enabledManual();
        integer(module, "maximum_retries").set(1);
        FakeWorld world = new FakeWorld();
        world.supported = position -> position.y() < 67;

        runToTerminal(module, pos(0, 64, 0),
                target("enemy", pos(1, 64, 0), 1.0D, false), world, true);

        assertEquals(DomainExpansion.State.CANCELLED, module.state());
        assertEquals(DomainExpansion.CancelReason.STRUCTURE_INCOMPLETE, module.cancelReason());
        assertTrue(module.renderSnapshot().blocks().stream()
                .anyMatch(block -> block.part() != DomainPlacementPlan.Part.WALL
                        && block.status() == DomainExpansion.PlacementStatus.FAILED));
    }

    @Test
    void insufficientBlocksAbortBeforeAnyPlacementRequest() {
        DomainExpansion module = enabledManual();
        DomainExpansion.TickResult result = module.tick(context(0, pos(0, 64, 0),
                List.of(target("enemy", pos(1, 64, 0), 1.0D, false)), 15, new FakeWorld()));

        assertEquals(DomainExpansion.CancelReason.INSUFFICIENT_BLOCKS, result.cancelReason());
        assertTrue(result.placements().isEmpty());
        assertEquals(0, module.attemptedPlacements());
    }

    @Test
    void targetLeavingStaticPlanCancelsConstruction() {
        DomainExpansion module = enabledManual();
        FakeWorld world = new FakeWorld();
        DomainTarget initial = target("enemy", pos(1, 64, 0), 1.0D, false);
        DomainExpansion.TickResult first = module.tick(context(0, pos(0, 64, 0),
                List.of(initial), 512, world));
        assertFalse(first.placements().isEmpty());

        DomainTarget escaped = target("enemy", pos(20, 64, 0), 20.0D, false);
        DomainExpansion.TickResult second = module.tick(context(1, pos(0, 64, 0),
                List.of(escaped), 512, world));
        assertEquals(DomainExpansion.CancelReason.TARGET_ESCAPED, second.cancelReason());
        assertEquals(DomainExpansion.State.CANCELLED, module.state());
    }

    @Test
    void localAndTargetHitboxesAreNeverReturnedAsPlacements() {
        DomainExpansion module = enabledManual();
        DomainPosition local = pos(0, 64, 0);
        DomainTarget target = target("enemy", pos(1, 64, 0), 1.0D, false);
        DomainBounds bounds = DomainBoundsCalculator.calculate(local, target.feet(), 0, 3, 10, 10).orElseThrow();
        DomainPlacementPlan plan = DomainPlanGenerator.generate(bounds, local, target, true, false,
                DomainPlanGenerator.ExitMode.NO_EXIT, true);
        DomainPosition nearLocalWall = plan.entries(DomainPlacementPlan.Part.WALL).getFirst().position();
        FakeWorld world = new FakeWorld();
        world.blocked.add(nearLocalWall);

        DomainExpansion.TickResult result = module.tick(context(0, local, List.of(target), 512, world));

        assertFalse(result.placements().contains(nearLocalWall));
        assertTrue(result.placements().stream().noneMatch(position ->
                position.equals(local) || position.equals(target.feet())));
    }

    @Test
    void friendNeverTriggersAutomaticProximityAndDwellIsRequired() {
        DomainExpansion module = new DomainExpansion();
        enumSetting(module, "activation_mode", DomainExpansion.ActivationMode.class)
                .set(DomainExpansion.ActivationMode.AUTOMATIC_PROXIMITY);
        integer(module, "target_delay").set(2);
        module.enable();
        FakeWorld world = new FakeWorld();
        DomainTarget friend = target("friend", pos(1, 64, 0), 1.0D, true);
        for (int tick = 0; tick < 4; tick++) {
            assertTrue(module.tick(context(tick, pos(0, 64, 0), List.of(friend), 512, world))
                    .placements().isEmpty());
            assertEquals(DomainExpansion.State.ARMED, module.state());
        }

        DomainTarget enemy = target("enemy", pos(1, 64, 0), 1.0D, false);
        assertTrue(module.tick(context(4, pos(0, 64, 0), List.of(enemy), 512, world))
                .placements().isEmpty());
        assertTrue(module.tick(context(5, pos(0, 64, 0), List.of(enemy), 512, world))
                .placements().isEmpty());
        assertFalse(module.tick(context(6, pos(0, 64, 0), List.of(enemy), 512, world))
                .placements().isEmpty());
    }

    @Test
    void automaticCooldownIsPerTarget() {
        DomainTargetSelector selector = new DomainTargetSelector();
        DomainTarget enemy = target("enemy", pos(1, 64, 0), 1.0D, false);
        DomainTargetSelector.Options options = new DomainTargetSelector.Options(true, true, false, true);

        assertTrue(selector.select(0, List.of(enemy), 4.0D, 0, options,
                DomainTargetSelector.Priority.NEAREST, true).isPresent());
        selector.coolDown(enemy.id(), 0, 10);
        assertTrue(selector.select(9, List.of(enemy), 4.0D, 0, options,
                DomainTargetSelector.Priority.NEAREST, true).isEmpty());
        assertTrue(selector.select(10, List.of(enemy), 4.0D, 0, options,
                DomainTargetSelector.Priority.NEAREST, true).isPresent());
    }

    @Test
    void killAuraStyleTargetCheckboxesFilterPlayersHostilesPassiveAndFriends() {
        DomainTargetSelector selector = new DomainTargetSelector();
        DomainTarget player = target("player", CombatEntityType.PLAYER, false);
        DomainTarget friend = target("friend", CombatEntityType.PLAYER, true);
        DomainTarget hostile = target("warden", CombatEntityType.HOSTILE, false);
        DomainTarget passive = target("cow", CombatEntityType.PASSIVE, false);

        DomainTargetSelector.Options defaults = new DomainTargetSelector.Options(true, true, false, true);
        assertEquals("player", selector.select(0, List.of(passive, friend, hostile, player),
                6.0D, 0, defaults, DomainTargetSelector.Priority.NEAREST, false).orElseThrow().id());
        assertTrue(selector.select(0, List.of(friend), 6.0D, 0, defaults,
                DomainTargetSelector.Priority.NEAREST, false).isEmpty());
        assertEquals("warden", selector.select(0, List.of(hostile), 6.0D, 0, defaults,
                DomainTargetSelector.Priority.NEAREST, false).orElseThrow().id());
        assertTrue(selector.select(0, List.of(passive), 6.0D, 0, defaults,
                DomainTargetSelector.Priority.NEAREST, false).isEmpty());

        DomainTargetSelector.Options passiveOnly = new DomainTargetSelector.Options(false, false, true, true);
        assertEquals("cow", selector.select(0, List.of(player, hostile, passive),
                6.0D, 0, passiveOnly, DomainTargetSelector.Priority.NEAREST, false).orElseThrow().id());

        DomainTargetSelector.Options friendsAllowed = new DomainTargetSelector.Options(true, false, false, false);
        assertEquals("friend", selector.select(0, List.of(friend), 6.0D, 0, friendsAllowed,
                DomainTargetSelector.Priority.NEAREST, false).orElseThrow().id());
    }

    @Test
    void manualDisableCancelsAndRunsRestorationHook() {
        DomainExpansion module = enabledManual();
        AtomicBoolean restored = new AtomicBoolean();
        module.setCleanupHook(() -> restored.set(true));
        module.tick(context(0, pos(0, 64, 0),
                List.of(target("enemy", pos(1, 64, 0), 1.0D, false)), 512, new FakeWorld()));

        module.disable();

        assertTrue(restored.get());
        assertFalse(module.isEnabled());
        assertEquals(DomainExpansion.State.IDLE, module.state());
    }

    @Test
    void placementRejectionsStopAfterConfiguredRetries() {
        DomainExpansion module = enabledManual();
        integer(module, "maximum_retries").set(0);
        integer(module, "blocks_per_tick").set(6);
        FakeWorld world = new FakeWorld();
        DomainPosition local = pos(0, 64, 0);
        DomainTarget target = target("enemy", pos(1, 64, 0), 1.0D, false);

        for (long tick = 0; tick < 300 && module.state() != DomainExpansion.State.CANCELLED; tick++) {
            DomainExpansion.TickResult result = module.tick(context(tick, local, List.of(target), 512, world));
            for (DomainPosition position : result.placements()) {
                module.recordPlacementAttempt(position, false, tick);
            }
        }

        assertEquals(DomainExpansion.State.CANCELLED, module.state());
        assertEquals(DomainExpansion.CancelReason.STRUCTURE_INCOMPLETE, module.cancelReason());
        assertTrue(module.attemptedPlacements() > 0);
        assertTrue(module.attemptedPlacements() <= 256);
    }

    @Test
    void selectedSlotOwnershipRestoresOnlyWhenStillOwned() {
        DomainInventoryOwnership ownership = new DomainInventoryOwnership();
        ownership.acquire(2, 5);
        assertEquals(2, ownership.originalSlot());
        assertEquals(5, ownership.ownedSlot());
        assertFalse(ownership.hasConflict(5));
        assertEquals(2, ownership.restorationSlot(5));

        ownership.acquire(1, 4);
        assertTrue(ownership.hasConflict(7));
        assertEquals(-1, ownership.restorationSlot(7));
    }

    @Test
    void manualFinalSealDefersDoorUntilASecondActivationPress() {
        DomainExpansion module = enabledManual();
        enumSetting(module, "leave_self_exit", DomainPlanGenerator.ExitMode.class)
                .set(DomainPlanGenerator.ExitMode.MANUAL_FINAL_SEAL);
        FakeWorld world = new FakeWorld();
        DomainPosition local = pos(0, 64, 0);
        DomainTarget target = target("enemy", pos(1, 64, 0), 1.0D, false);
        runToTerminal(module, local, target, world, true);
        int before = module.renderSnapshot().blocks().size();

        assertTrue(module.consumesKeybindInput());
        assertTrue(module.requestFinalSeal());
        assertEquals(DomainExpansion.State.PLACE_WALLS, module.state());
        assertEquals(before + 2, module.renderSnapshot().blocks().size());
        assertNotEquals(1.0D, module.completion());
    }

    private static void runToTerminal(DomainExpansion module, DomainPosition local, DomainTarget target,
                                      FakeWorld world, boolean accept) {
        for (long tick = 0; tick < 600
                && module.state() != DomainExpansion.State.COMPLETE
                && module.state() != DomainExpansion.State.CANCELLED; tick++) {
            DomainExpansion.TickResult result = module.tick(context(tick, local, List.of(target), 512, world));
            for (DomainPosition placement : result.placements()) {
                if (accept) {
                    world.solid.add(placement);
                }
                module.recordPlacementAttempt(placement, accept, tick);
            }
        }
    }

    private static DomainExpansion.Context context(long tick, DomainPosition local, List<DomainTarget> targets,
                                                   int blocks, FakeWorld world) {
        return new DomainExpansion.Context(tick, true, local, targets, blocks, false, false, world);
    }

    private static DomainExpansion enabledManual() {
        DomainExpansion module = new DomainExpansion();
        module.enable();
        return module;
    }

    private static DomainTarget target(String id, DomainPosition feet, double distance, boolean friend) {
        return new DomainTarget(id, id, CombatEntityType.PLAYER, feet, feet.x() + 0.5D, feet.z() + 0.5D,
                distance, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D,
                friend, true, false, false, true, false);
    }

    private static DomainTarget target(String id, CombatEntityType type, boolean friend) {
        DomainPosition feet = pos(1, 64, 0);
        return new DomainTarget(id, id, type, feet, feet.x() + 0.5D, feet.z() + 0.5D,
                1.0D, 20.0D, 0.0D, 0.0D, 0.0D, 1.0D, 0.0D,
                friend, true, false, false, true, false);
    }

    private static DomainPosition pos(int x, int y, int z) {
        return new DomainPosition(x, y, z);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> EnumSetting<E> enumSetting(DomainExpansion module, String id, Class<E> type) {
        return (EnumSetting<E>) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static IntegerSetting integer(DomainExpansion module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static BooleanSetting bool(DomainExpansion module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static final class FakeWorld implements DomainExpansion.WorldView {
        private final Set<DomainPosition> solid = new HashSet<>();
        private final Set<DomainPosition> blocked = new HashSet<>();
        private Predicate<DomainPosition> supported = ignored -> true;

        @Override
        public boolean loaded(DomainPosition position) {
            return true;
        }

        @Override
        public boolean solid(DomainPosition position) {
            return solid.contains(position);
        }

        @Override
        public boolean replaceable(DomainPosition position) {
            return !solid.contains(position);
        }

        @Override
        public boolean liquid(DomainPosition position) {
            return false;
        }

        @Override
        public boolean supported(DomainPosition position) {
            return supported.test(position);
        }

        @Override
        public boolean reachable(DomainPosition position) {
            return true;
        }

        @Override
        public boolean intersectsProtectedEntity(DomainPosition position) {
            return blocked.contains(position);
        }
    }
}
