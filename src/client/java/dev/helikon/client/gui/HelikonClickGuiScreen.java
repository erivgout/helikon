package dev.helikon.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** The deliberately small, local-only ClickGUI placeholder for the bootstrap milestone. */
public final class HelikonClickGuiScreen extends Screen {
    private static final Component MESSAGE = Component.translatable("screen.helikon.placeholder");

    public HelikonClickGuiScreen() {
        super(Component.translatable("screen.helikon.title"));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int x = (width - font.width(MESSAGE)) / 2;
        graphics.text(font, MESSAGE, x, height / 2, 0xFFFFFFFF, true);
    }
}
