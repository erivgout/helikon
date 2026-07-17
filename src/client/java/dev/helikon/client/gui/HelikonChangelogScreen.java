package dev.helikon.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/** Small local screen for bundled update notes. */
public final class HelikonChangelogScreen extends Screen {
    private final List<String> lines;

    public HelikonChangelogScreen(List<String> lines) {
        super(Component.literal("Helikon Changelog"));
        this.lines = List.copyOf(lines);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractMenuBackground(graphics);
        int x = Math.max(20, width / 2 - 170);
        int y = Math.max(20, height / 2 - lines.size() * 7);
        graphics.text(font, "Helikon Changelog", x, y, 0xFFFFFFFF, true);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), x, y + 20 + index * 13, 0xFFDDDDDD, false);
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }
}
