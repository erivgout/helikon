package dev.helikon.client.mixin;

import dev.helikon.client.command.ChatCommands;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Gives only the local dot-command prefix precedence over vanilla Tab handling. */
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin {
    @Shadow
    protected EditBox input;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void helikon$completeLocalCommand(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        if (event.key() == GLFW.GLFW_KEY_TAB && ChatCommands.complete(input)) {
            callback.setReturnValue(true);
        }
    }
}
