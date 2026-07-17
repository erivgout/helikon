package dev.helikon.client.module.movement;

import dev.helikon.client.mixin.MinecraftAccessor;
import dev.helikon.client.module.world.BuildPoint;
import dev.helikon.client.module.world.BuildVector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Thin 26.2 client bridge for conservative advanced-movement decisions. */
public final class MinecraftAdvancedMovementAccess {
    private MinecraftAdvancedMovementAccess() {
    }

    public static void tickFastLadders(FastLadders module) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client)) {
            return;
        }
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        module.verticalVelocity(player.onClimbable(), player.input.keyPresses.forward(), player.input.keyPresses.jump(),
                        player.input.keyPresses.shift(), velocity.y)
                .ifPresent(y -> player.setDeltaMovement(velocity.x, y, velocity.z));
    }

    public static void tickSpeed(Speed speed) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client) || client.player.isFallFlying() || client.player.onClimbable()) {
            return;
        }
        LocalPlayer player = client.player;
        Vec2 input = player.input.getMoveVector();
        boolean moving = input.x != 0.0F || input.y != 0.0F;
        Vec3 velocity = player.getDeltaMovement();
        HorizontalVelocity current = new HorizontalVelocity(velocity.x, velocity.z);
        HorizontalVelocity adjusted = speed.adjust(current, desiredDirection(player, input), moving);
        if (!adjusted.equals(current)) {
            player.setDeltaMovement(adjusted.x(), velocity.y, adjusted.z());
        }
    }

    public static void tickBunnyHop(BunnyHop bunnyHop) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client) || client.player.isFallFlying()) {
            return;
        }
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        HorizontalVelocity current = new HorizontalVelocity(velocity.x, velocity.z);
        HorizontalVelocity capped = bunnyHop.cap(current);
        if (!capped.equals(current)) {
            player.setDeltaMovement(capped.x(), velocity.y, capped.z());
        }
    }

    public static void tickFlight(Flight module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            FreecamAccess.stop(client);
            module.onContextLost();
            return;
        }
        if (client.gui.screen() != null && module.isEnabled()) {
            return;
        }
        FreecamAccess.tick(client);
        LocalPlayer player = client.player;
        Abilities abilities = player.getAbilities();
        Flight.Action action = module.update(new Flight.Abilities(abilities.mayfly, abilities.flying, abilities.getFlyingSpeed()));
        if (!action.setFlying() && !action.setSpeed()) {
            return;
        }
        if (action.setFlying()) {
            abilities.flying = action.flying();
        }
        if (action.setSpeed()) {
            abilities.setFlyingSpeed(action.flyingSpeed());
        }
        player.onUpdateAbilities();
    }

    public static void tickNoFall(NoFall module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            module.onContextLost();
            return;
        }
        LocalPlayer player = client.player;
        Abilities abilities = player.getAbilities();
        NoFall.Action action = module.update(player.fallDistance, abilities.mayfly, player.onGround(), abilities.flying,
                isInteractive(client));
        if (!action.setFlying()) {
            return;
        }
        abilities.flying = action.flying();
        player.onUpdateAbilities();
    }

    public static void tickElytra(ExtraElytra module) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client) || !client.player.isFallFlying()) {
            return;
        }
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        float adjusted = module.adjustedPitch(player.getXRot(), true, velocity.y < 0.0D,
                player.getAvailableSpaceBelow(24.0D));
        if (adjusted != player.getXRot()) {
            player.setXRot(adjusted);
        }
    }

    /** Requests one normal held-block placement only while Use is held and vanilla's cooldown is clear. */
    public static void tickScaffold(Scaffold module, long tick) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client) || client.gameMode == null || !client.options.keyUse.isDown()
                || ((MinecraftAccessor) client).helikon$getRightClickDelay() != 0) {
            return;
        }
        LocalPlayer player = client.player;
        if (!(player.getInventory().getSelectedItem().getItem() instanceof BlockItem)) {
            selectHotbarBlock(module, player);
            return;
        }
        Vec2 input = player.input.getMoveVector();
        boolean moving = input.x != 0.0F || input.y != 0.0F;
        Direction forwardDirection = Direction.fromYRot(player.getYRot());
        BuildPoint playerBlock = point(player.blockPosition());
        BuildVector forward = vector(forwardDirection);
        BuildPoint targetPoint = module.candidateTarget(playerBlock, forward, moving);
        BlockPos target = blockPos(targetPoint);
        if (!isReplaceableLoaded(client, target)) {
            return;
        }
        Optional<BlockHitResult> hit = placementHit(client, target);
        if (hit.isEmpty() || module.nextTarget(tick, playerBlock, forward, moving, true).isEmpty()) {
            return;
        }
        if (module.rotateToTarget()) {
            player.setYRot(Direction.getYRot(hit.get().getDirection()));
        }
        client.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit.get());
    }

    public static ExtraElytra.Status elytraStatus(ExtraElytra module, LocalPlayer player) {
        Vec3 velocity = player.getDeltaMovement();
        return module.status(new HorizontalVelocity(velocity.x, velocity.z), velocity.y,
                remainingDurability(player.getItemBySlot(EquipmentSlot.CHEST)));
    }

    private static void selectHotbarBlock(Scaffold module, LocalPlayer player) {
        List<Scaffold.HotbarBlock> candidates = new ArrayList<>();
        for (int slot = 0; slot < 9; slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof BlockItem && !stack.isEmpty()) {
                candidates.add(new Scaffold.HotbarBlock(slot, stack.getCount()));
            }
        }
        module.selectBlockSlot(player.getInventory().getSelectedSlot(), false, candidates)
                .ifPresent(player.getInventory()::setSelectedSlot);
    }

    private static HorizontalVelocity desiredDirection(LocalPlayer player, Vec2 input) {
        Vec3 forward = player.getViewVector(1.0F);
        double x = forward.x * input.y - forward.z * input.x;
        double z = forward.z * input.y + forward.x * input.x;
        return new HorizontalVelocity(x, z);
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

    private static boolean isInteractive(Minecraft client) {
        return client.player != null && client.level != null && client.gui.screen() == null;
    }

    private static int remainingDurability(ItemStack stack) {
        return stack.isDamageableItem() ? Math.max(0, stack.getMaxDamage() - stack.getDamageValue()) : Integer.MAX_VALUE;
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
}
