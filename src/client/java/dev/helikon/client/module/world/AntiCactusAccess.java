package dev.helikon.client.module.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Narrow 26.2 adapter that reads only loaded local cactus collision boxes. */
public final class AntiCactusAccess {
    private static final int MAXIMUM_SCANNED_BLOCKS = 64;
    private static final double MAXIMUM_ADJUSTABLE_COMPONENT = 1.0D;
    private static volatile AntiCactus module;

    private AntiCactusAccess() {
    }

    public static void install(AntiCactus antiCactus) {
        module = Objects.requireNonNull(antiCactus, "antiCactus");
    }

    /** Leaves world-driven, unbounded, or unknown-chunk local movement untouched. */
    public static Vec3 adjustMovement(LocalPlayer player, MoverType moverType, Vec3 movement) {
        AntiCactus current = module;
        if (current == null || !current.shouldAdjust(moverType == MoverType.SELF) || player.isSpectator()
                || player.isPassenger() || !player.isAlive() || !movement.isFinite()
                || exceedsLocalBound(movement)) {
            return movement;
        }

        AABB playerBox = player.getBoundingBox();
        ClientLevel level = (ClientLevel) player.level();
        List<CactusCollisionPolicy.Bounds> cactusBoxes = cactusCollisionBoxes(level, playerBox.expandTowards(movement));
        if (cactusBoxes.isEmpty()) {
            return movement;
        }
        CactusCollisionPolicy.Movement adjusted = current.adjust(movement(movement), bounds(playerBox), cactusBoxes);
        return new Vec3(adjusted.x(), adjusted.y(), adjusted.z());
    }

    private static boolean exceedsLocalBound(Vec3 movement) {
        return Math.abs(movement.x()) > MAXIMUM_ADJUSTABLE_COMPONENT
                || Math.abs(movement.y()) > MAXIMUM_ADJUSTABLE_COMPONENT
                || Math.abs(movement.z()) > MAXIMUM_ADJUSTABLE_COMPONENT;
    }

    private static List<CactusCollisionPolicy.Bounds> cactusCollisionBoxes(ClientLevel level, AABB sweptBox) {
        List<CactusCollisionPolicy.Bounds> boxes = new ArrayList<>();
        int scanned = 0;
        for (BlockPos position : BlockPos.betweenClosed(sweptBox)) {
            if (++scanned > MAXIMUM_SCANNED_BLOCKS) {
                return List.of();
            }
            if (!level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                return List.of();
            }
            BlockState state = level.getBlockState(position);
            if (!state.is(Blocks.CACTUS)) {
                continue;
            }
            for (AABB collisionBox : state.getCollisionShape(level, position).toAabbs()) {
                boxes.add(bounds(collisionBox.move(position)));
            }
        }
        return boxes;
    }

    private static CactusCollisionPolicy.Movement movement(Vec3 value) {
        return new CactusCollisionPolicy.Movement(value.x(), value.y(), value.z());
    }

    private static CactusCollisionPolicy.Bounds bounds(AABB value) {
        return new CactusCollisionPolicy.Bounds(value.minX, value.minY, value.minZ,
                value.maxX, value.maxY, value.maxZ);
    }
}
