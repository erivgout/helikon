package dev.helikon.client.mixin;

import dev.helikon.client.chat.ChatDisplayAccess;
import dev.helikon.client.chat.BetterChatDisplayAccess;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds optional local timestamp labels after Minecraft has logged the original chat content. */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent")
abstract class ChatComponentMixin {
    @ModifyArg(
            method = "addMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToDisplayQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"),
            index = 0
    )
    private GuiMessage helikon$timestampDisplayMessage(GuiMessage message) {
        return ChatDisplayAccess.decorateTimestamp(BetterChatDisplayAccess.decorateClickableNames(message));
    }

    @ModifyArg(
            method = "addMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V"),
            index = 0
    )
    private GuiMessage helikon$timestampStoredMessage(GuiMessage message) {
        ChatComponent chat = (ChatComponent) (Object) this;
        GuiMessage stacked = BetterChatDisplayAccess.stackStoredMessage(chat, message);
        return ChatDisplayAccess.decorateTimestamp(BetterChatDisplayAccess.decorateClickableNames(stacked));
    }

    @Inject(
            method = "addMessage",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/components/ChatComponent;addMessageToQueue(Lnet/minecraft/client/multiplayer/chat/GuiMessage;)V", shift = At.Shift.AFTER)
    )
    private void helikon$rebuildAfterDuplicateStack(CallbackInfo callback) {
        if (BetterChatDisplayAccess.consumeRescaleRequested()) {
            ((ChatComponent) (Object) this).rescaleChat();
        }
    }

    @ModifyConstant(method = "addMessageToDisplayQueue", constant = @Constant(intValue = 100))
    private int helikon$expandTrimmedHistory(int vanillaLimit) {
        return BetterChatDisplayAccess.historyLimit();
    }

    @ModifyConstant(method = "addMessageToQueue", constant = @Constant(intValue = 100))
    private int helikon$expandStoredHistory(int vanillaLimit) {
        return BetterChatDisplayAccess.historyLimit();
    }

    @ModifyConstant(method = "getLineHeight", constant = @Constant(doubleValue = 9.0D))
    private double helikon$applyCompactLineHeight(double vanillaLineHeight) {
        return BetterChatDisplayAccess.lineHeight(vanillaLineHeight);
    }

    @Inject(method = "scrollChat", at = @At("HEAD"), cancellable = true)
    private void helikon$smoothLocalScroll(int lines, CallbackInfo callback) {
        if (BetterChatDisplayAccess.requestSmoothScroll((ChatComponent) (Object) this, lines)) {
            callback.cancel();
        }
    }

    /**
     * 26.2 uses this local for both direct queue/restricted prompt fills and
     * captures it in the ordinary-line background lambda before its own
     * {@code ARGB.black} call.
     */
    @ModifyVariable(
            method = "extractRenderState(Lnet/minecraft/client/gui/components/ChatComponent$ChatGraphicsAccess;IILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;)V",
            at = @At(value = "STORE"),
            index = 13
    )
    private float helikon$applyLocalTextBackgroundOpacity(float vanillaOpacity) {
        return ChatDisplayAccess.backgroundOpacity(vanillaOpacity);
    }
}
