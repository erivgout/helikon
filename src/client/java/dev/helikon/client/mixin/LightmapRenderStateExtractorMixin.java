package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Removes only AntiBlind-configured Darkness contribution from the local lightmap. */
@Mixin(targets = "net.minecraft.client.renderer.LightmapRenderStateExtractor")
abstract class LightmapRenderStateExtractorMixin {
    @Redirect(
            method = "extract",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F")
    )
    private float helikon$hideDarknessLightmap(LocalPlayer player, Holder<MobEffect> effect, float tickDelta) {
        if (effect == MobEffects.DARKNESS && RenderModuleAccess.hideMobEffectFog(effect)) {
            return 0.0F;
        }
        return player.getEffectBlendFactor(effect, tickDelta);
    }
}
