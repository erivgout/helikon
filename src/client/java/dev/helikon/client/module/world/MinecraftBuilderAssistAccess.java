package dev.helikon.client.module.world;

import dev.helikon.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow 26.2 bridge from BuilderAssist's local plan to a normal held-block use interaction.
 * It neither changes inventory state nor builds packets; Minecraft validates every attempted placement.
 */
public final class MinecraftBuilderAssistAccess {
    private MinecraftBuilderAssistAccess() {
    }

    /** Executes at most one ordinary block-use interaction when Use is held and its vanilla cooldown is clear. */
    public static void tick(BuilderAssist builderAssist, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())
                || !client.options.keyUse.isDown()
                || ((MinecraftAccessor) client).helikon$getRightClickDelay() != 0
                || !(client.player.getInventory().getSelectedItem().getItem() instanceof BlockItem)) {
            return;
        }
        Optional<AnchorContext> context = anchorContext(client, builderAssist);
        if (context.isEmpty()) {
            return;
        }
        AnchorContext anchorContext = context.get();
        builderAssist.nextAction(tick, new BuilderAssist.Context(true, true, anchorContext.anchor(),
                        anchorContext.placementHits().keySet()))
                .map(anchorContext.placementHits()::get)
                .ifPresent(hit -> client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit));
    }

    /** Returns only loaded, replaceable plan blocks for the renderer's local preview. */
    public static List<BlockPos> previewPositions(Minecraft client, BuilderAssist builderAssist) {
        if (client.player == null || client.level == null
                || !(client.player.getInventory().getSelectedItem().getItem() instanceof BlockItem)) {
            return List.of();
        }
        return anchorContext(client, builderAssist).map(context -> context.placementHits().keySet().stream()
                .map(MinecraftBuilderAssistAccess::blockPos).toList()).orElseGet(List::of);
    }

    private static Optional<AnchorContext> anchorContext(Minecraft client, BuilderAssist builderAssist) {
        if (!(client.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return Optional.empty();
        }
        Direction forward = Direction.fromYRot(client.player.getYRot());
        if (forward.getAxis().isVertical()) {
            return Optional.empty();
        }
        BlockPos target = hit.getBlockPos().relative(hit.getDirection());
        BuilderPlan.Anchor anchor = new BuilderPlan.Anchor(point(target), vector(forward.getClockWise()), vector(forward));
        Map<BuildPoint, BlockHitResult> placements = new LinkedHashMap<>();
        for (BuildPoint point : builderAssist.preview(anchor)) {
            BlockPos blockPos = blockPos(point);
            if (!isReplaceableLoaded(client, blockPos)) {
                continue;
            }
            placementHit(client, hit, blockPos).ifPresent(placement -> placements.put(point, placement));
        }
        return Optional.of(new AnchorContext(anchor, Map.copyOf(placements)));
    }

    private static Optional<BlockHitResult> placementHit(Minecraft client, BlockHitResult originalHit, BlockPos target) {
        if (originalHit.getBlockPos().relative(originalHit.getDirection()).equals(target)) {
            return Optional.of(originalHit);
        }
        for (Direction face : Direction.values()) {
            BlockPos support = target.relative(face.getOpposite());
            if (!isReplaceableLoaded(client, support)) {
                return Optional.of(new BlockHitResult(Vec3.atCenterOf(support), face, support, false));
            }
        }
        return Optional.empty();
    }

    private static boolean isReplaceableLoaded(Minecraft client, BlockPos position) {
        return client.level.isInsideBuildHeight(position.getY())
                && client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
                && client.level.getBlockState(position).canBeReplaced();
    }

    private static BuildPoint point(BlockPos position) {
        return new BuildPoint(position.getX(), position.getY(), position.getZ());
    }

    private static BlockPos blockPos(BuildPoint point) {
        return new BlockPos(point.x(), point.y(), point.z());
    }

    private static BuildVector vector(Direction direction) {
        return new BuildVector(direction.getStepX(), direction.getStepY(), direction.getStepZ());
    }

    private record AnchorContext(BuilderPlan.Anchor anchor, Map<BuildPoint, BlockHitResult> placementHits) {
    }
}
