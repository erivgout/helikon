package dev.helikon.client.mixin;

import dev.helikon.client.module.miscellaneous.BookHackAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.BookEditScreen;
import net.minecraft.client.input.KeyEvent;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** Adds one explicit, bounded bulk-paste gesture to the vanilla editor. */
@Mixin(BookEditScreen.class)
abstract class BookEditScreenMixin {
    @Shadow
    private List<String> pages;

    @Shadow
    private void updatePageContent() {
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void helikon$bulkPaste(KeyEvent event, CallbackInfoReturnable<Boolean> callback) {
        int required = GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SHIFT;
        if (event.key() != GLFW.GLFW_KEY_V || (event.modifiers() & required) != required) {
            return;
        }
        List<String> imported = BookHackAccess.paginate(Minecraft.getInstance().keyboardHandler.getClipboard());
        if (imported.isEmpty()) {
            return;
        }
        pages.clear();
        pages.addAll(imported);
        updatePageContent();
        callback.setReturnValue(true);
    }
}
