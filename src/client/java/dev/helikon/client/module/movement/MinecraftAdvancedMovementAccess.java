package dev.helikon.client.module.movement;

import dev.helikon.client.mixin.MinecraftAccessor;
import dev.helikon.client.module.world.BuildPoint;
import dev.helikon.client.module.world.BuildVector;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

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

    /** Holds an eligible local player at water level while keeping every vanilla opt-out intact. */
    public static void tickJesus(Jesus module) {
        Minecraft client = Minecraft.getInstance();
        if (!isInteractive(client)) {
            return;
        }
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        OptionalDouble surface = waterSurfaceY(client, player);
        module.surfaceHeight(surface.isPresent(), player.input.keyPresses.shift(), player.input.keyPresses.jump(),
                        player.isPassenger(), player.getAbilities().flying, player.isFallFlying(), player.getY(),
                        surface.orElse(player.getY()), velocity.y)
                .ifPresent(y -> {
                    player.setPos(player.getX(), y, player.getZ());
                    player.setDeltaMovement(velocity.x, 0.0D, velocity.z);
                    player.setOnGround(true);
                    player.fallDistance = 0.0F;
                });
    }

    public static void tickSpider(Spider module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        Vec2 input = player.input.getMoveVector();
        boolean moving = input.x != 0.0F || input.y != 0.0F;
        Vec3 velocity = player.getDeltaMovement();
        module.verticalVelocity(client.gui.screen() != null, player.horizontalCollision, moving,
                        player.onClimbable(), player.input.keyPresses.shift(), player.isPassenger(),
                        player.getAbilities().flying, player.isFallFlying(), velocity.y)
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
            module.onContextLost();
            return;
        }
        LocalPlayer player = client.player;
        boolean screenOpen = client.gui.screen() != null;
        if (screenOpen && module.isEnabled()) {
            // Hold a stable player hover so opening a screen does not drop the player.
            if (!player.isPassenger() && module.usesVelocityFlight(player.getAbilities().mayfly)) {
                player.setDeltaMovement(0.0D, 0.0D, 0.0D);
            }
            return;
        }
        Abilities abilities = player.getAbilities();
        Flight.Action action = module.update(new Flight.Abilities(abilities.mayfly, abilities.flying, abilities.getFlyingSpeed()));
        if (action.setFlying()) {
            abilities.flying = action.flying();
        }
        if (action.setSpeed()) {
            abilities.setFlyingSpeed(action.flyingSpeed());
        }
        if (action.setFlying() || action.setSpeed()) {
            player.onUpdateAbilities();
        }
        if (!player.isPassenger() && module.usesVelocityFlight(abilities.mayfly)) {
            Vec2 input = player.input.getMoveVector();
            Flight.FlightVelocity velocity = module.flightVelocity(desiredDirection(player, input),
                    player.input.keyPresses.jump(), player.input.keyPresses.shift());
            player.setDeltaMovement(velocity.x(), velocity.y(), velocity.z());
        }
    }

    public static void tickBoatFlight(BoatFlight module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        AbstractBoat boat = rideableBoat(module, client.player);
        if (boat == null) {
            return;
        }
        if (client.gui.screen() != null) {
            boat.setDeltaMovement(0.0D, 0.0D, 0.0D);
            return;
        }
        LocalPlayer player = client.player;
        Vec2 input = player.input.getMoveVector();
        BoatFlight.Velocity velocity = module.velocity(desiredDirection(player, input),
                player.input.keyPresses.jump(), player.input.keyPresses.shift());
        boat.setDeltaMovement(velocity.x(), velocity.y(), velocity.z());
    }

    /** The ridden boat only when Boat Flight is enabled and the local player is its driver. */
    private static AbstractBoat rideableBoat(BoatFlight module, LocalPlayer player) {
        if (module.isEnabled() && player.getVehicle() instanceof AbstractBoat boat
                && boat.getControllingPassenger() == player) {
            return boat;
        }
        return null;
    }

    public static void tickNoFall(NoFall module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        Abilities abilities = player.getAbilities();
        if (module.shouldResetFall(player.fallDistance, player.onGround(), player.isPassenger(),
                abilities.flying, player.isFallFlying())) {
            player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, false));
            player.fallDistance = 0.0F;
        }
    }

    /** Applies Glide's capped descent only during an ordinary local fall. */
    public static void tickGlide(Glide module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        LocalPlayer player = client.player;
        Vec3 velocity = player.getDeltaMovement();
        module.verticalVelocity(client.gui.screen() != null, player.onGround(), player.isInWater(), player.onClimbable(),
                        player.input.keyPresses.shift(), player.isPassenger(), player.getAbilities().flying,
                        player.isFallFlying(), velocity.y)
                .ifPresent(y -> player.setDeltaMovement(velocity.x, y, velocity.z));
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
        return MovementDirections.fromView(forward.x, forward.z, input.x, input.y);
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

    /** Finds only the top of a loaded water column at the player's horizontal position. */
    private static OptionalDouble waterSurfaceY(Minecraft client, LocalPlayer player) {
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        int topCandidateY = (int) Math.floor(player.getY() + 0.01D);
        for (int offset = 0; offset <= 1; offset++) {
            BlockPos position = new BlockPos(x, topCandidateY - offset, z);
            if (!client.level.isLoaded(position)) {
                continue;
            }
            var fluid = client.level.getFluidState(position);
            if (fluid.is(FluidTags.WATER) && !client.level.getFluidState(position.above()).is(FluidTags.WATER)) {
                return OptionalDouble.of(position.getY() + fluid.getHeight(client.level, position));
            }
        }
        return OptionalDouble.empty();
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
