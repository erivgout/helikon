package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.AntiWaterPushAccess;
import dev.helikon.client.module.movement.NoSlowAccess;
import dev.helikon.client.module.world.AntiCactusAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityFluidInteraction;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Limits NoSlow and AntiCactus collision changes strictly to the local client player. */
@Mixin(Entity.class)
abstract class EntityMovementMixin {
    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Vec3 helikon$avoidCactusCollision(Vec3 movement, MoverType moverType) {
        if ((Object) this instanceof LocalPlayer player) {
            movement = AntiCactusAccess.adjustMovement(player, moverType, movement);
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

    @Redirect(
            method = "updateFluidInteraction",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityFluidInteraction;"
                    + "applyCurrentTo(Lnet/minecraft/tags/TagKey;Lnet/minecraft/world/entity/Entity;D)V")
    )
    private void helikon$blockLocalWaterCurrent(EntityFluidInteraction interaction,
                                                TagKey<Fluid> fluid,
                                                Entity entity, double scale) {
        if (FluidTags.WATER.equals(fluid) && entity instanceof LocalPlayer
                && AntiWaterPushAccess.blocksWaterCurrent()) {
            return;
        }
        interaction.applyCurrentTo(fluid, entity, scale);
    }
}
