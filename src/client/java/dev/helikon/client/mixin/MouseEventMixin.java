package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts normalized raw mouse observations without cancelling or consuming input. */
@Mixin(MouseHandler.class)
abstract class MouseEventMixin {
    @Inject(method = "onButton", at = @At("TAIL"))
    private void helikon$postMouseButtonEvent(long window, MouseButtonInfo event, int action, CallbackInfo callback) {
        ClientEventAccess.postMouseButton(action, event);
    }

    @Inject(method = "onScroll", at = @At("TAIL"))
    private void helikon$postMouseScrollEvent(long window, double scrollX, double scrollY, CallbackInfo callback) {
        ClientEventAccess.postMouseScroll(scrollX, scrollY);
    }
}
