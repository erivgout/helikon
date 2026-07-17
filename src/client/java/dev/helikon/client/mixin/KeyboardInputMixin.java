package dev.helikon.client.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.module.movement.MovementModuleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
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
        input.keyPresses = MovementModuleAccess.applyMovement(
                input.keyPresses,
                client.gui.screen() != null,
                MovementModuleAccess.isAutoSneakKeyDown(key -> InputConstants.isKeyDown(client.getWindow(), key))
        );
        var vector = MovementModuleAccess.movementVector(input.keyPresses);
        ((ClientInputAccessor) input).helikon$setMoveVector(new Vec2(vector.x(), vector.y()));
    }
}
