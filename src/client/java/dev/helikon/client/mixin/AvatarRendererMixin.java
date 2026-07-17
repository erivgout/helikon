package dev.helikon.client.mixin;

import dev.helikon.client.render.LocalCapeRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Substitutes a generated cape only after vanilla has extracted the local avatar render state. */
@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
            at = @At("TAIL"))
    private void helikon$applyLocalCape(Avatar avatar, AvatarRenderState renderState, float tickProgress,
                                        CallbackInfo callback) {
        LocalCapeRenderer.apply(avatar, renderState);
    }
}
