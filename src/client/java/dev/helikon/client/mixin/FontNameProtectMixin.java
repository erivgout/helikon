package dev.helikon.client.mixin;

import dev.helikon.client.module.render.NameProtectTextAccess;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Replaces only render/measurement inputs in the verified 26.2 font pipeline. */
@Mixin(Font.class)
abstract class FontNameProtectMixin {
    @ModifyVariable(
            method = "prepareText(Ljava/lang/String;FFIZI)Lnet/minecraft/client/gui/Font$PreparedText;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String helikon$protectPreparedString(String text) {
        return NameProtectTextAccess.protect(text);
    }

    @ModifyVariable(
            method = {
                    "prepare8xTextOutline(Lnet/minecraft/util/FormattedCharSequence;FFI)Lnet/minecraft/client/gui/Font$PreparedText;",
                    "prepareText(Lnet/minecraft/util/FormattedCharSequence;FFIZZI)Lnet/minecraft/client/gui/Font$PreparedText;"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private FormattedCharSequence helikon$protectPreparedSequence(FormattedCharSequence text) {
        return NameProtectTextAccess.protect(text);
    }

    @ModifyVariable(method = "width(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private String helikon$protectStringWidth(String text) {
        return NameProtectTextAccess.protect(text);
    }

    @ModifyVariable(
            method = "width(Lnet/minecraft/util/FormattedCharSequence;)I",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private FormattedCharSequence helikon$protectSequenceWidth(FormattedCharSequence text) {
        return NameProtectTextAccess.protect(text);
    }

    @ModifyVariable(
            method = {
                    "width(Lnet/minecraft/network/chat/FormattedText;)I",
                    "substrByWidth(Lnet/minecraft/network/chat/FormattedText;I)Lnet/minecraft/network/chat/FormattedText;",
                    "split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;",
                    "splitIgnoringLanguage(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private FormattedText helikon$protectFormattedText(FormattedText text) {
        return NameProtectTextAccess.protect(text);
    }

    @ModifyVariable(
            method = {
                    "plainSubstrByWidth(Ljava/lang/String;IZ)Ljava/lang/String;",
                    "plainSubstrByWidth(Ljava/lang/String;I)Ljava/lang/String;"
            },
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private String helikon$protectClippedString(String text) {
        return NameProtectTextAccess.protect(text);
    }
}
