package dev.helikon.client.mixin;

import dev.helikon.client.chat.ChatDisplayAccess;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Adds optional local timestamp labels after Minecraft has logged the original chat content. */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent")
abstract class ChatComponentMixin {
    @ModifyArg(
            method = "addMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"),
            index = 0
    )
    private GuiMessage helikon$timestampDisplayMessage(GuiMessage message) {
        return ChatDisplayAccess.decorateTimestamp(message);
    }

    @ModifyArg(
            method = "addMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"),
            index = 0
    )
    private GuiMessage helikon$timestampStoredMessage(GuiMessage message) {
        return ChatDisplayAccess.decorateTimestamp(message);
    }
}
