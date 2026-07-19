package dev.helikon.client.module.world;

import dev.helikon.client.mixin.MultiPlayerGameModeAccessor;
import dev.helikon.client.module.movement.Timer;
import dev.helikon.client.module.player.ToolCandidate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Narrow 26.2 adapter for already-loaded, normal block-destroy input paths. */
public final class MinecraftMiningAccess {
    private static final int MAXIMUM_LINE_OF_SIGHT_CANDIDATES = 32;
    private static final NukerBreakSequence NUKER_BREAK_SEQUENCE = new NukerBreakSequence();

    private MinecraftMiningAccess() {
    }

    /** Lets FastBreak lower only a visible, ordinary held-attack cooldown. */
    public static void tickFastBreak(FastBreak module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !client.level.isLoaded(hit.getBlockPos())) {
            module.tick(false, false, "");
            return;
        }
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        boolean attackHeld = client.options.keyAttack.isDown();
        String id = blockId(state);
        module.tick(attackHeld, !state.isAir(), id);
        int extraSteps = module.extraDestroySteps(attackHeld, !state.isAir(), id);
        if (state.getDestroySpeed(client.level, hit.getBlockPos()) < 0.0F || !client.gameMode.isDestroying()) {
            return;
        }
        for (int step = 0; step < extraSteps && !client.level.getBlockState(hit.getBlockPos()).isAir(); step++) {
            client.gameMode.continueDestroyBlock(hit.getBlockPos(), hit.getDirection());
        }
    }

    /** Applies Timer's optional multiplier only to Minecraft's active ordinary digging path. */
    public static void tickTimerDigging(Timer module) {
        Minecraft client = Minecraft.getInstance();
        if (!module.usesDiggingOnlyMode() || client.player == null || client.level == null
                || client.gameMode == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())
                || !(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK
                || !client.level.isLoaded(hit.getBlockPos())) {
            module.extraDiggingSteps(false);
            return;
        }
        BlockState state = client.level.getBlockState(hit.getBlockPos());
        boolean activeDigging = client.options.keyAttack.isDown() && !state.isAir()
                && state.getDestroySpeed(client.level, hit.getBlockPos()) >= 0.0F
                && client.gameMode.isDestroying();
        int extraSteps = module.extraDiggingSteps(activeDigging);
        for (int step = 0; step < extraSteps && !client.level.getBlockState(hit.getBlockPos()).isAir(); step++) {
            client.gameMode.continueDestroyBlock(hit.getBlockPos(), hit.getDirection());
        }
    }

    /** Selects at most two loaded, reachable targets and invokes Minecraft's ordinary destroy interaction for each. */
    public static void tickNuker(Nuker module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null) {
            NUKER_BREAK_SEQUENCE.reset();
            return;
        }
        LocalPlayer player = client.player;
        boolean screenOpen = dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen());
        Nuker.Context context = new Nuker.Context(screenOpen, client.options.keyAttack.isDown());
        if (!module.shouldScan(context)) {
            NUKER_BREAK_SEQUENCE.reset();
            applyToolAction(player, module.toolAction(false, player.getInventory().getSelectedSlot(), List.of()));
            return;
        }
        List<Candidate> candidates = candidates(client, module);
        List<Nuker.Target> targets = module.selectTargets(context, lineOfSightFacts(client, module, candidates));
        applyToolAction(player, module.toolAction(!targets.isEmpty(), player.getInventory().getSelectedSlot(),
                targets.isEmpty() ? List.of() : toolCandidates(player, client.level.getBlockState(blockPos(targets.getFirst())))));
        if (targets.isEmpty() || screenOpen) {
            NUKER_BREAK_SEQUENCE.reset();
            return;
        }

        int actionCount = player.isCreative() ? targets.size() : 1;
        for (int index = 0; index < actionCount; index++) {
            Nuker.Target target = targets.get(index);
            BlockPos position = blockPos(target);
            if (!client.level.isLoaded(position) || !player.isWithinBlockInteractionRange(position, 0.0D)
                    || client.level.getBlockState(position).isAir()) {
                continue;
            }
            if (module.rotatesToTarget()) {
                Vec3 eye = player.getEyePosition();
                NukerRotation.Rotation rotation = NukerRotation.toward(eye.x, eye.y, eye.z,
                        target.x(), target.y(), target.z());
                player.setYRot(rotation.yaw());
                player.setXRot(rotation.pitch());
            }
            // Minecraft's normal API applies its own prediction and packet format. Resetting only this transient
            // client cooldown lets the module's separately bounded action cap take effect without fabricating packets.
            ((MultiPlayerGameModeAccessor) client.gameMode).helikon$setDestroyDelay(0);
            Direction face = targetFace(client, position);
            if (player.isCreative() || NUKER_BREAK_SEQUENCE.next(position.asLong()) == NukerBreakSequence.Action.START) {
                client.gameMode.startDestroyBlock(position, face);
            } else {
                client.gameMode.continueDestroyBlock(position, face);
            }
        }
        if (player.isCreative()) {
            NUKER_BREAK_SEQUENCE.reset();
        }
    }

    private static List<Candidate> candidates(Minecraft client, Nuker module) {
        LocalPlayer player = client.player;
        int radius = module.radius();
        BlockPos center = player.blockPosition();
        List<Candidate> candidates = new ArrayList<>();
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (!client.level.isLoaded(position) || !player.isWithinBlockInteractionRange(position, 0.0D)) {
                continue;
            }
            BlockState state = client.level.getBlockState(position);
            if (state.isAir() || !module.isConfiguredTarget(blockId(state))) {
                continue;
            }
            candidates.add(new Candidate(position.immutable(), blockId(state),
                    Nuker.squaredDistanceFromPlayer(player.getX(), player.getY(), player.getZ(),
                            position.getX(), position.getY(), position.getZ())));
        }
        candidates.sort(Comparator.comparingDouble(Candidate::squaredDistance).thenComparing(Candidate::blockId)
                .thenComparing(candidate -> candidate.position().asLong()));
        return candidates;
    }

    private static List<Nuker.Target> lineOfSightFacts(Minecraft client, Nuker module, List<Candidate> candidates) {
        int limit = module.lineOfSightRequired() ? Math.min(MAXIMUM_LINE_OF_SIGHT_CANDIDATES, candidates.size())
                : candidates.size();
        List<Nuker.Target> targets = new ArrayList<>(limit);
        for (int index = 0; index < limit; index++) {
            Candidate candidate = candidates.get(index);
            BlockPos position = candidate.position();
            targets.add(new Nuker.Target(position.getX(), position.getY(), position.getZ(), candidate.blockId(),
                    candidate.squaredDistance(), !module.lineOfSightRequired() || hasLineOfSight(client, position),
                    client.level.getBlockState(position).getDestroySpeed(client.level, position) >= 0.0F));
        }
        return targets;
    }

    private static boolean hasLineOfSight(Minecraft client, BlockPos position) {
        BlockHitResult hit = client.level.clip(new ClipContext(client.player.getEyePosition(), Vec3.atCenterOf(position),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position);
    }

    private static Direction targetFace(Minecraft client, BlockPos position) {
        BlockHitResult hit = client.level.clip(new ClipContext(client.player.getEyePosition(), Vec3.atCenterOf(position),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(position) ? hit.getDirection() : Direction.UP;
    }

    private static List<ToolCandidate> toolCandidates(LocalPlayer player, BlockState state) {
        List<ToolCandidate> candidates = new ArrayList<>(9);
        for (int slot = 0; slot < 9; slot++) {
            var stack = player.getInventory().getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int remainingDurability = stack.isDamageableItem()
                    ? stack.getMaxDamage() - stack.getDamageValue()
                    : Integer.MAX_VALUE;
            candidates.add(new ToolCandidate(slot, stack.getDestroySpeed(state), stack.isCorrectToolForDrops(state),
                    remainingDurability));
        }
        return candidates;
    }

    private static void applyToolAction(LocalPlayer player, Nuker.ToolAction action) {
        if (action.type() == Nuker.ToolActionType.SELECT || action.type() == Nuker.ToolActionType.RESTORE) {
            player.getInventory().setSelectedSlot(action.slot());
        }
    }

    private static BlockPos blockPos(Nuker.Target target) {
        return new BlockPos(target.x(), target.y(), target.z());
    }

    private static String blockId(BlockState state) {
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private record Candidate(BlockPos position, String blockId, double squaredDistance) {
    }
}
