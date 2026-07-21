package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides vanilla living-entity labels only when Better Nametags draws its complete local replacement. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererBetterNametagMixin {
    @Inject(
            method = "extractNameTags(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/client/renderer/entity/state/EntityRenderState;F)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void helikon$replaceVanillaNametag(Entity entity, EntityRenderState state, float partialTick,
                                               CallbackInfo callback) {
        if (RenderModuleAccess.hideVanillaNametag(entity)) {
            callback.cancel();
        }
    }
}
