package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import dev.helikon.client.event.RenderEvent;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes vanilla block-outline submission without altering its render state. */
@Mixin(LevelRenderer.class)
abstract class LevelRendererEventMixin {
    @Inject(method = "submitBlockOutline", at = @At("HEAD"))
    private void helikon$observeBlockOutlineRender(CallbackInfo callback) {
        ClientEventAccess.postRender(RenderEvent.Kind.BLOCK_OUTLINE, 0.0D, "");
    }
}
