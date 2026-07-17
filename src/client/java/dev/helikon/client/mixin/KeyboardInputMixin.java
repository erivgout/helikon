package dev.helikon.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.module.movement.AdvancedMovementInputAccess;
import dev.helikon.client.module.movement.AntiAfk;
import dev.helikon.client.module.movement.AntiAfkAccess;
import dev.helikon.client.module.movement.AutoParkour;
import dev.helikon.client.module.movement.FreecamAccess;
import dev.helikon.client.module.movement.InventoryWalkAccess;
import dev.helikon.client.module.movement.MovementModuleAccess;
import dev.helikon.client.module.movement.ParkourAccess;
import dev.helikon.client.module.movement.ParkourContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies local movement modules after Minecraft 26.2 has freshly polled physical movement keys. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void helikon$applyMovementModules(CallbackInfo callback) {
        KeyboardInput input = (KeyboardInput) (Object) this;
        Minecraft client = Minecraft.getInstance();
        Input physicalInput = input.keyPresses;
        Screen screen = client.gui.screen();
        boolean screenOpen = screen != null;
        input.keyPresses = MovementModuleAccess.applyMovement(
                input.keyPresses,
                screenOpen,
                MovementModuleAccess.isAutoSneakKeyDown(key -> InputConstants.isKeyDown(client.getWindow(), key))
        );
        boolean ordinaryInventoryScreen = screen instanceof InventoryScreen;
        Input inventoryPhysicalInput = ordinaryInventoryScreen ? physicalInventoryInput(client) : physicalInput;
        input.keyPresses = InventoryWalkAccess.apply(input.keyPresses, inventoryPhysicalInput,
                ordinaryInventoryScreen, hasFocusedWidget(screen));
        var initialVector = MovementModuleAccess.movementVector(input.keyPresses);
        boolean openBelow = client.player != null
                && client.player.level().getBlockState(client.player.blockPosition().below()).canBeReplaced();
        input.keyPresses = AdvancedMovementInputAccess.apply(input.keyPresses, screenOpen,
                client.player != null && client.player.onGround(), initialVector.x() != 0.0F || initialVector.y() != 0.0F,
                openBelow, client.options.keyUse.isDown());
        if (ParkourAccess.shouldJump(parkourContext(client, input.keyPresses, screenOpen))) {
            input.keyPresses = withJump(input.keyPresses);
        }
        AntiAfk.Action antiAfkAction = AntiAfkAccess.tick(new AntiAfk.Context(screenOpen,
                hasManualInput(physicalInput), client.player != null && client.player.onGround()));
        if (antiAfkAction.yawDegrees() != 0.0F && client.player != null) {
            client.player.setYRot(client.player.getYRot() + antiAfkAction.yawDegrees());
        }
        if (antiAfkAction.jump() || antiAfkAction.moveForward()) {
            input.keyPresses = withAntiAfkAction(input.keyPresses, antiAfkAction);
        }
        input.keyPresses = FreecamAccess.captureAndSuppress(input.keyPresses);
        var vector = MovementModuleAccess.movementVector(input.keyPresses);
        ((ClientInputAccessor) input).helikon$setMoveVector(new Vec2(vector.x(), vector.y()));
    }

    /** Uses physical configured keyboard bindings only while the player's vanilla inventory is open. */
    private static Input physicalInventoryInput(Minecraft client) {
        return new Input(
                isPhysicalKeyboardKeyDown(client, client.options.keyUp),
                isPhysicalKeyboardKeyDown(client, client.options.keyDown),
                isPhysicalKeyboardKeyDown(client, client.options.keyLeft),
                isPhysicalKeyboardKeyDown(client, client.options.keyRight),
                isPhysicalKeyboardKeyDown(client, client.options.keyJump),
                false,
                isPhysicalKeyboardKeyDown(client, client.options.keySprint)
        );
    }

    private static boolean isPhysicalKeyboardKeyDown(Minecraft client, KeyMapping mapping) {
        if (mapping.isUnbound()) {
            return false;
        }
        InputConstants.Key key = ((KeyMappingAccessor) (Object) mapping).helikon$getKey();
        return key.getType() == InputConstants.Type.KEYSYM
                && InputConstants.isKeyDown(client.getWindow(), key.getValue());
    }

    private static boolean hasFocusedWidget(Screen screen) {
        return screen != null && screen.children().stream().anyMatch(GuiEventListener::isFocused);
    }

    private static ParkourContext parkourContext(Minecraft client, Input input, boolean screenOpen) {
        if (client.player == null || client.level == null) {
            return new ParkourContext(screenOpen, false, false, 0.0D, false, false,
                    AutoParkour.MAXIMUM_SAFE_DROP_BLOCKS + 1, false);
        }
        BlockPos targetFeet = client.player.blockPosition().relative(client.player.getDirection());
        LedgeFacts facts = ledgeFacts(client, targetFeet);
        return new ParkourContext(screenOpen, client.player.onGround(), input.forward() && !input.backward(),
                client.player.getDeltaMovement().horizontalDistance(), facts.ledgeAhead(), facts.lavaAhead(),
                facts.dropBlocks(), facts.landingSupportsPlayer());
    }

    /** Reads only loaded blocks in front of the local player; no chunk lookup or interaction is requested. */
    private static LedgeFacts ledgeFacts(Minecraft client, BlockPos targetFeet) {
        if (!client.level.isLoaded(targetFeet) || !client.level.isLoaded(targetFeet.above())
                || !client.level.isLoaded(targetFeet.below())) {
            return LedgeFacts.UNSAFE;
        }
        BlockState feetState = client.level.getBlockState(targetFeet);
        BlockState headState = client.level.getBlockState(targetFeet.above());
        BlockState firstSupport = client.level.getBlockState(targetFeet.below());
        boolean lava = isLava(feetState) || isLava(headState) || isLava(firstSupport);
        if (!feetState.canBeReplaced() || !headState.canBeReplaced() || !firstSupport.canBeReplaced()) {
            return new LedgeFacts(false, lava, 0, false);
        }
        for (int depth = 2; depth <= AutoParkour.MAXIMUM_SAFE_DROP_BLOCKS + 1; depth++) {
            BlockPos candidate = targetFeet.below(depth);
            if (!client.level.isLoaded(candidate)) {
                return LedgeFacts.UNSAFE;
            }
            BlockState state = client.level.getBlockState(candidate);
            lava |= isLava(state);
            if (!state.canBeReplaced()) {
                if (state.isFaceSturdy(client.level, candidate, Direction.UP)) {
                    return new LedgeFacts(true, lava, depth - 1, true);
                }
                return LedgeFacts.UNSAFE;
            }
        }
        return new LedgeFacts(true, lava, AutoParkour.MAXIMUM_SAFE_DROP_BLOCKS + 1, false);
    }

    private static boolean isLava(BlockState state) {
        Fluid fluid = state.getFluidState().getType();
        return fluid == Fluids.LAVA || fluid == Fluids.FLOWING_LAVA;
    }

    private static boolean hasManualInput(Input input) {
        return input.forward() || input.backward() || input.left() || input.right()
                || input.jump() || input.shift() || input.sprint();
    }

    private static Input withJump(Input input) {
        return new Input(input.forward(), input.backward(), input.left(), input.right(), true,
                input.shift(), input.sprint());
    }

    private static Input withAntiAfkAction(Input input, AntiAfk.Action action) {
        return new Input(input.forward() || action.moveForward(), input.backward(), input.left(), input.right(),
                input.jump() || action.jump(), input.shift(), input.sprint());
    }

    private record LedgeFacts(boolean ledgeAhead, boolean lavaAhead, int dropBlocks,
                              boolean landingSupportsPlayer) {
        private static final LedgeFacts UNSAFE = new LedgeFacts(false, false,
                AutoParkour.MAXIMUM_SAFE_DROP_BLOCKS + 1, false);
    }
}
