package dev.helikon.client.module.world;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Narrow 26.2 bridge from AirPlace's local plan to one ordinary held-block use interaction.
 * It builds a vanilla-shaped {@link BlockHitResult} for a loaded replaceable target and lets
 * Minecraft and the server validate it; it changes no inventory state and forges no packets.
 */
public final class MinecraftAirPlaceAccess {
    private MinecraftAirPlaceAccess() {
    }

    /** Executes at most one air-placement use when Use is held and a block is selected. */
    public static void tick(AirPlace airPlace, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.gameMode == null || client.gui.screen() != null
                || !client.options.keyUse.isDown()
                || !(client.player.getInventory().getSelectedItem().getItem() instanceof BlockItem)) {
            return;
        }

        Vec3 eye = client.player.getEyePosition();
        Vec3 view = client.player.getViewVector(1.0F);
        AirPlace.Ray ray = new AirPlace.Ray(eye.x, eye.y, eye.z, view.x, view.y, view.z);
        boolean hasBlockTarget = client.hitResult != null && client.hitResult.getType() == HitResult.Type.BLOCK;

        Optional<AirPlace.Placement> planned = airPlace.plan(tick,
                new AirPlace.Input(true, true, hasBlockTarget, ray));
        if (planned.isEmpty()) {
            return;
        }
        AirPlace.Placement placement = planned.get();
        BlockPos target = new BlockPos(placement.block().x(), placement.block().y(), placement.block().z());
        if (!isReplaceableLoaded(client, target)) {
            return;
        }
        Direction face = direction(placement.face());
        BlockHitResult hit = new BlockHitResult(
                new Vec3(placement.hitX(), placement.hitY(), placement.hitZ()), face, target, false);
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND, hit);
    }

    private static boolean isReplaceableLoaded(Minecraft client, BlockPos position) {
        return client.level.isInsideBuildHeight(position.getY())
                && client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4)
                && client.level.getBlockState(position).canBeReplaced();
    }

    private static Direction direction(BuildVector vector) {
        for (Direction candidate : Direction.values()) {
            if (candidate.getStepX() == vector.x() && candidate.getStepY() == vector.y()
                    && candidate.getStepZ() == vector.z()) {
                return candidate;
            }
        }
        throw new IllegalArgumentException("No Direction matches vector " + vector);
    }
}
