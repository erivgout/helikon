package dev.helikon.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Recolors only the local item-stack foil vertices after Minecraft selects its glint buffer. */
@Mixin(targets = "net.minecraft.client.renderer.feature.ItemFeatureRenderer")
abstract class ItemFeatureRendererMixin {
    @Inject(method = "getFoilBuffer", at = @At("RETURN"), cancellable = true)
    private void helikon$tintItemGlint(RenderType renderType, PoseStack.Pose pose,
                                       CallbackInfoReturnable<VertexConsumer> callback) {
        if (RenderModuleAccess.rainbowEnchantEnabled()) {
            callback.setReturnValue(new RainbowEnchantVertexConsumer(callback.getReturnValue(),
                    RenderModuleAccess.rainbowEnchantColor(System.currentTimeMillis())));
        }
    }
}
