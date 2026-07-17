package dev.helikon.client.mixin;

import dev.helikon.client.module.render.ChamsRenderAccess;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Chams silhouette colors after vanilla has extracted each entity render state. */
@Mixin(EntityRenderer.class)
abstract class EntityRendererChamsOutlineMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void helikon$applyChamsColor(Entity entity, EntityRenderState state, float partialTick,
                                         CallbackInfo callback) {
        ChamsRenderAccess.colorFor(entity.getId()).ifPresent(color -> state.outlineColor = color);
    }
}
