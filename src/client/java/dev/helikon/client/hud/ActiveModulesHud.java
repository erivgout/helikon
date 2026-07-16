package dev.helikon.client.hud;

import dev.helikon.client.module.ModuleRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

import java.util.List;
import java.util.Objects;

/** Thin Fabric HUD bridge for the enabled-module list. */
public final class ActiveModulesHud implements HudElement {
    public static final int PADDING = 3;
    public static final int COLOR_BACKGROUND = 0xB014161B;
    public static final int COLOR_TEXT = 0xFFE8A33D;

    private final ModuleRegistry modules;
    private final HudLayout layout;

    public ActiveModulesHud(ModuleRegistry modules, HudLayout layout) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!layout.activeModulesEnabled()) {
            return;
        }

        List<String> names = ActiveModules.enabledNames(modules);
        if (names.isEmpty()) {
            return;
        }

        draw(graphics, Minecraft.getInstance().font, names,
                bounds(Minecraft.getInstance().font, names, layout.activeModulesX(), layout.activeModulesY()),
                COLOR_TEXT, false);
    }

    /** Calculates the complete draggable footprint, including the text backdrop. */
    public static HudBounds bounds(Font font, List<String> lines, int x, int y) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Active Modules HUD requires at least one preview line");
        }

        int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        return new HudBounds(x, y, textWidth + PADDING * 2, lines.size() * font.lineHeight + PADDING * 2);
    }

    /** Draws the shared normal-HUD or editor-preview representation. */
    public static void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            List<String> lines,
            HudBounds bounds,
            int textColor,
            boolean selected
    ) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(bounds, "bounds");

        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), COLOR_BACKGROUND);
        if (selected) {
            graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), COLOR_TEXT);
        }
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(font, lines.get(index), bounds.x() + PADDING,
                    bounds.y() + PADDING + index * font.lineHeight, textColor, true);
        }
    }
}
