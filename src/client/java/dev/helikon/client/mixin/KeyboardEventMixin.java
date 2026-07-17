package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Posts normalized raw keyboard observations without changing Minecraft input handling. */
@Mixin(KeyboardHandler.class)
abstract class KeyboardEventMixin {
    @Inject(method = "keyPress", at = @At("TAIL"))
    private void helikon$postKeyEvent(long window, int action, KeyEvent event, CallbackInfo callback) {
        ClientEventAccess.postKey(action, event);
    }
}
