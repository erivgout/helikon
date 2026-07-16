package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.MovementModuleAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies AutoWalk after Minecraft 26.2 has freshly polled physical movement keys. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin {
    @Inject(method = "tick", at = @At("TAIL"))
    private void helikon$applyAutoWalk(CallbackInfo callback) {
        KeyboardInput input = (KeyboardInput) (Object) this;
        input.keyPresses = MovementModuleAccess.applyAutoWalk(
                input.keyPresses, Minecraft.getInstance().gui.screen() != null
        );
        var vector = MovementModuleAccess.movementVector(input.keyPresses);
        ((ClientInputAccessor) input).helikon$setMoveVector(new Vec2(vector.x(), vector.y()));
    }
}
