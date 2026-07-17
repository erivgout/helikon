package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.NoSlowAccess;
import dev.helikon.client.module.movement.StepAccess;
import dev.helikon.client.module.world.AntiCactusAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limits NoSlow and Step collision changes strictly to the local client player. */
@Mixin(Entity.class)
abstract class EntityMovementMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 helikon$avoidCactusCollision(Vec3 movement, MoverType moverType) {
        if ((Object) this instanceof LocalPlayer player) {
            return AntiCactusAccess.adjustMovement(player, moverType, movement);
        }
        return movement;
    }

    @Inject(method = "getBlockSpeedFactor", at = @At("RETURN"), cancellable = true)
    private void helikon$ignoreConfiguredBlockSlowdown(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof LocalPlayer player && NoSlowAccess.ignoreBlockFactor(player)) {
            callback.setReturnValue(1.0F);
        }
    }

    @Inject(method = "makeStuckInBlock", at = @At("HEAD"), cancellable = true)
    private void helikon$ignoreConfiguredCobweb(BlockState state, Vec3 multiplier, CallbackInfo callback) {
        if ((Object) this instanceof LocalPlayer && state.is(net.minecraft.world.level.block.Blocks.COBWEB)
                && NoSlowAccess.ignoreCobweb()) {
            callback.cancel();
        }
    }

    @Inject(method = "maxUpStep", at = @At("RETURN"), cancellable = true)
    private void helikon$applyLocalStepHeight(CallbackInfoReturnable<Float> callback) {
        if ((Object) this instanceof LocalPlayer) {
            callback.setReturnValue(StepAccess.height(callback.getReturnValue()));
        }
    }
}
