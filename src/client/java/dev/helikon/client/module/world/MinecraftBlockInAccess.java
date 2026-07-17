package dev.helikon.client.module.world;

import dev.helikon.client.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Narrow 26.2 bridge from BlockIn's local plan to ordinary held-block use interactions.
 * It neither edits inventory state nor builds packets; Minecraft validates every attempted placement.
 */
public final class MinecraftBlockInAccess {
    private MinecraftBlockInAccess() {
    }

    /** Places up to the module's bounded plan while a held block is selected and vanilla's use cooldown is clear. */
    public static void tick(BlockIn blockIn, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null
                || ((MinecraftAccessor) client).helikon$getRightClickDelay() != 0
                || !(client.player.getInventory().getSelectedItem().getItem() instanceof BlockItem)) {
            return;
        }
        BuildPoint feet = point(client.player.blockPosition());
        Map<BuildPoint, BlockHitResult> placements = new LinkedHashMap<>();
        for (BuildPoint target : blockIn.targets(feet)) {
            BlockPos blockPos = blockPos(target);
            if (!isReplaceableLoaded(client, blockPos)) {
                continue;
            }
            placementHit(client, blockPos).ifPresent(hit -> placements.put(target, hit));
        }
        for (BuildPoint placement : blockIn.nextPlacements(tick, new BlockIn.Context(feet, placements.keySet()))) {
            client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, placements.get(placement));
        }
    }

    private static Optional<BlockHitResult> placementHit(Minecraft client, BlockPos target) {
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
}
