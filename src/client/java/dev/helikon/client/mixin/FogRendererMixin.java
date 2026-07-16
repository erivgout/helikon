package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Filters only Blindness and Darkness fog environments selected by AntiBlind. */
@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
abstract class FogRendererMixin {
    @Redirect(
            method = {"setupFog", "computeFogColor"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/fog/environment/FogEnvironment;isApplicable(Lnet/minecraft/world/level/material/FogType;Lnet/minecraft/world/entity/Entity;)Z")
    )
    private boolean helikon$filterImpairmentFog(FogEnvironment environment, FogType fogType, Entity entity) {
        if (environment instanceof MobEffectFogEnvironment effectEnvironment
                && RenderModuleAccess.hideMobEffectFog(effectEnvironment.getMobEffect())) {
            return false;
        }
        return environment.isApplicable(fogType, entity);
    }
}
