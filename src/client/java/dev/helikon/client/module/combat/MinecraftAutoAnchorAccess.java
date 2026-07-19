package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.mixin.MinecraftAccessor;
import dev.helikon.client.mixin.MultiPlayerGameModeAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Thin 26.2 adapter for loaded-block scans and ordinary anchor interactions. */
public final class MinecraftAutoAnchorAccess {
    private static final int MAX_TARGETS = 8;
    private static final int USE_COOLDOWN_TICKS = 4;

    private MinecraftAutoAnchorAccess() {
    }

    /** Performs at most one normal place, charge, or detonate interaction. */
    public static boolean tick(long tick, AutoAnchor module, MinecraftCombatAccess.Snapshot snapshot) {
        Minecraft client = Minecraft.getInstance();
        if (!snapshot.available() || client.player == null || client.level == null || client.gameMode == null
                || dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen())) {
            return false;
        }

        LocalPlayer player = client.player;
        Vec3 eye = player.getEyePosition();
        List<Vec3> targets = eligibleTargets(module, snapshot, eye);
        if (targets.isEmpty()) {
            return false;
        }
        List<Vec3> friends = friendPositions(snapshot, eye);
        HotbarSlots slots = hotbarSlots(player);
        CandidateScan scan = scanCandidates(client, module, eye, targets, friends,
                slots.anchorSlot() >= 0 && module.placeEnabled());
        AutoAnchor.State state = new AutoAnchor.State(
                ((MinecraftAccessor) client).helikon$getRightClickDelay() == 0,
                true,
                slots.anchorSlot(),
                slots.glowstoneSlot(),
                slots.emptySlot(),
                scan.anchors(),
                scan.placements()
        );
        AutoAnchor.Action action = module.decide(tick, state);
        boolean acted = switch (action.type()) {
            case PLACE -> place(client, action);
            case CHARGE -> useAnchor(client, action, Items.GLOWSTONE, false);
            case DETONATE -> useAnchor(client, action, null, true);
            case NONE -> false;
        };
        if (acted) {
            ((MinecraftAccessor) client).helikon$setRightClickDelay(USE_COOLDOWN_TICKS);
        }
        return acted;
    }

    private static boolean place(Minecraft client, AutoAnchor.Action action) {
        BlockPos target = new BlockPos(action.x(), action.y(), action.z());
        if (!isLoaded(client, target) || !client.level.getBlockState(target).canBeReplaced()
                || !anchorExplodes(client, target)) {
            return false;
        }
        Optional<BlockHitResult> hit = placementHit(client, target);
        return hit.isPresent() && visible(client, hit.get().getBlockPos(), hit.get().getLocation())
                && interactTemporarily(client, action.hotbarSlot(), Items.RESPAWN_ANCHOR, hit.get());
    }

    private static boolean useAnchor(Minecraft client, AutoAnchor.Action action, Item requiredItem,
                                     boolean requireEmpty) {
        BlockPos position = new BlockPos(action.x(), action.y(), action.z());
        if (!isLoaded(client, position) || !anchorExplodes(client, position)) {
            return false;
        }
        BlockState state = client.level.getBlockState(position);
        if (!state.is(Blocks.RESPAWN_ANCHOR)) {
            return false;
        }
        int charges = state.getValue(RespawnAnchorBlock.CHARGE);
        if ((requireEmpty && charges == 0) || (!requireEmpty && charges >= RespawnAnchorBlock.MAX_CHARGES)) {
            return false;
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(position), Direction.UP, position, false);
        return visible(client, position, hit.getLocation())
                && interactTemporarily(client, action.hotbarSlot(), requiredItem, hit);
    }

    private static boolean interactTemporarily(Minecraft client, int slot, Item requiredItem, BlockHitResult hit) {
        LocalPlayer player = client.player;
        ItemStack actionStack = player.getInventory().getItem(slot);
        if ((requiredItem == null && !actionStack.isEmpty())
                || (requiredItem != null && actionStack.getItem() != requiredItem)) {
            return false;
        }

        int previousSlot = player.getInventory().getSelectedSlot();
        if (previousSlot != slot) {
            player.getInventory().setSelectedSlot(slot);
        }
        try {
            InteractionResult result = client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            if (result.consumesAction()) {
                player.swing(InteractionHand.MAIN_HAND);
            }
            return !(result instanceof InteractionResult.Fail);
        } finally {
            if (previousSlot != slot && player.getInventory().getSelectedSlot() == slot) {
                player.getInventory().setSelectedSlot(previousSlot);
                ((MultiPlayerGameModeAccessor) client.gameMode).helikon$ensureHasSentCarriedItem();
            }
        }
    }

    private static CandidateScan scanCandidates(Minecraft client, AutoAnchor module, Vec3 eye, List<Vec3> targets,
                                                List<Vec3> friends, boolean scanPlacements) {
        int radius = (int) Math.ceil(module.interactRange());
        BlockPos origin = BlockPos.containing(eye.x, eye.y, eye.z);
        List<AutoAnchor.Anchor> anchors = new ArrayList<>();
        List<AutoAnchor.Placement> placements = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos position = origin.offset(dx, dy, dz);
                    if (!isLoaded(client, position)) {
                        continue;
                    }
                    BlockState state = client.level.getBlockState(position);
                    Vec3 center = Vec3.atCenterOf(position);
                    double playerDistance = eye.distanceTo(center);
                    if (playerDistance > module.interactRange()) {
                        continue;
                    }
                    double targetDistance = nearestDistance(center, targets);
                    double friendDistance = nearestDistance(center, friends);
                    boolean explosive = anchorExplodes(client, position);
                    if (state.is(Blocks.RESPAWN_ANCHOR)) {
                        anchors.add(new AutoAnchor.Anchor(position.getX(), position.getY(), position.getZ(),
                                state.getValue(RespawnAnchorBlock.CHARGE), playerDistance, targetDistance,
                                friendDistance, explosive));
                    } else if (scanPlacements && state.canBeReplaced() && placementHit(client, position).isPresent()) {
                        placements.add(new AutoAnchor.Placement(position.getX(), position.getY(), position.getZ(),
                                playerDistance, targetDistance, friendDistance, explosive));
                    }
                }
            }
        }
        return new CandidateScan(anchors, placements);
    }

    private static List<Vec3> eligibleTargets(AutoAnchor module, MinecraftCombatAccess.Snapshot snapshot, Vec3 eye) {
        return snapshot.targets().stream()
                .filter(CombatTarget::alive)
                .filter(target -> target.distance() <= module.targetRange())
                .filter(target -> !(target.friend() && module.excludeFriends()))
                .filter(target -> target.type() == CombatEntityType.PLAYER && module.allowPlayers()
                        || target.type() == CombatEntityType.HOSTILE && module.allowHostiles())
                .sorted(Comparator.comparingDouble(CombatTarget::distance))
                .limit(MAX_TARGETS)
                .map(target -> eye.add(target.relativeX(), target.relativeY(), target.relativeZ()))
                .toList();
    }

    private static List<Vec3> friendPositions(MinecraftCombatAccess.Snapshot snapshot, Vec3 eye) {
        return snapshot.targets().stream()
                .filter(target -> target.alive() && target.friend())
                .map(target -> eye.add(target.relativeX(), target.relativeY(), target.relativeZ()))
                .toList();
    }

    private static HotbarSlots hotbarSlots(LocalPlayer player) {
        int anchor = -1;
        int glowstone = -1;
        int empty = -1;
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (anchor < 0 && stack.getItem() == Items.RESPAWN_ANCHOR) {
                anchor = slot;
            }
            if (glowstone < 0 && stack.getItem() == Items.GLOWSTONE) {
                glowstone = slot;
            }
            if (empty < 0 && stack.isEmpty()) {
                empty = slot;
            }
        }
        return new HotbarSlots(anchor, glowstone, empty);
    }

    private static Optional<BlockHitResult> placementHit(Minecraft client, BlockPos target) {
        for (Direction face : Direction.values()) {
            BlockPos support = target.relative(face.getOpposite());
            if (isLoaded(client, support) && !client.level.getBlockState(support).canBeReplaced()) {
                Vec3 location = Vec3.atCenterOf(support).add(
                        face.getStepX() * 0.5D,
                        face.getStepY() * 0.5D,
                        face.getStepZ() * 0.5D
                );
                return Optional.of(new BlockHitResult(location, face, support, false));
            }
        }
        return Optional.empty();
    }

    private static boolean visible(Minecraft client, BlockPos expected, Vec3 destination) {
        BlockHitResult hit = client.level.clip(new ClipContext(client.player.getEyePosition(), destination,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, client.player));
        return hit.getType() == HitResult.Type.BLOCK && hit.getBlockPos().equals(expected);
    }

    private static boolean anchorExplodes(Minecraft client, BlockPos position) {
        return !client.level.environmentAttributes().getValue(EnvironmentAttributes.RESPAWN_ANCHOR_WORKS, position);
    }

    private static boolean isLoaded(Minecraft client, BlockPos position) {
        return client.level.isInsideBuildHeight(position.getY())
                && client.level.hasChunk(position.getX() >> 4, position.getZ() >> 4);
    }

    private static double nearestDistance(Vec3 point, List<Vec3> positions) {
        double nearest = Double.MAX_VALUE;
        for (Vec3 position : positions) {
            nearest = Math.min(nearest, point.distanceTo(position));
        }
        return nearest;
    }

    private record HotbarSlots(int anchorSlot, int glowstoneSlot, int emptySlot) {
    }

    private record CandidateScan(List<AutoAnchor.Anchor> anchors, List<AutoAnchor.Placement> placements) {
    }
}
