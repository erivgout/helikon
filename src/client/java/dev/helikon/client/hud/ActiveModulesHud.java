package dev.helikon.client.hud;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Thin Fabric HUD bridge for the enabled-module list. */
public final class ActiveModulesHud implements HudElement {
    public static final int PADDING = 3;
    public static final int COLOR_BACKGROUND = 0xB014161B;
    public static final int COLOR_TEXT = 0xFFE8A33D;

    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final PanicState panicState;
    private final BooleanSupplier reducedAnimations;
    private final Map<String, Long> visibleSince = new HashMap<>();

    public ActiveModulesHud(ModuleRegistry modules, HudLayout layout) {
        this(modules, layout, new PanicState());
    }

    public ActiveModulesHud(ModuleRegistry modules, HudLayout layout, PanicState panicState) {
        this(modules, layout, panicState, () -> false);
    }

    public ActiveModulesHud(ModuleRegistry modules, HudLayout layout, PanicState panicState,
                            BooleanSupplier reducedAnimations) {
        this.modules = Objects.requireNonNull(modules, "modules");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.reducedAnimations = Objects.requireNonNull(reducedAnimations, "reducedAnimations");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        ActiveModulesLayout style = layout.activeModules();
        if (panicState.customHudHidden() || !style.enabled()) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        List<String> names = ActiveModules.enabledNames(modules, style.sort(), font::width);
        if (names.isEmpty()) {
            visibleSince.clear();
            return;
        }

        HudBounds bounds = bounds(font, names, style.x(), style.y(), style.padding());
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x(), bounds.y());
        graphics.pose().scale(style.scale());
        long tick = Minecraft.getInstance().level == null
                ? System.currentTimeMillis() / 50L
                : Minecraft.getInstance().level.getGameTime();
        int color = style.colorMode() == ActiveModulesLayout.ColorMode.RAINBOW
                ? ActiveModules.rainbowColor(tick)
                : COLOR_TEXT;
        long now = System.currentTimeMillis();
        if (!style.animations() || reducedAnimations.getAsBoolean()) {
            visibleSince.clear();
            draw(graphics, font, names, new HudBounds(0, 0, bounds.width(), bounds.height()), color, false,
                    style.padding(), style.alignment(), style.background(), style.textShadow());
        } else {
            names.forEach(name -> visibleSince.putIfAbsent(name, now));
            visibleSince.keySet().removeIf(name -> !names.contains(name));
            drawAnimated(graphics, font, names, new HudBounds(0, 0, bounds.width(), bounds.height()), color,
                    style.padding(), style.alignment(), style.background(), style.textShadow(), now);
        }
        graphics.pose().popMatrix();
    }

    /** Calculates the complete draggable footprint, including the text backdrop. */
    public static HudBounds bounds(Font font, List<String> lines, int x, int y) {
        return bounds(font, lines, x, y, PADDING);
    }

    public static HudBounds bounds(Font font, List<String> lines, int x, int y, int padding) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(lines, "lines");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Active Modules HUD requires at least one preview line");
        }
        if (padding < ActiveModulesLayout.MIN_PADDING || padding > ActiveModulesLayout.MAX_PADDING) {
            throw new IllegalArgumentException("Invalid padding");
        }

        int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        return new HudBounds(x, y, textWidth + padding * 2, lines.size() * font.lineHeight + padding * 2);
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
        draw(graphics, font, lines, bounds, textColor, selected, PADDING,
                ActiveModulesLayout.Alignment.LEFT, true, true);
    }

    /** Draws one Active Modules list with its persisted presentation controls. */
    public static void draw(
            GuiGraphicsExtractor graphics,
            Font font,
            List<String> lines,
            HudBounds bounds,
            int textColor,
            boolean selected,
            int padding,
            ActiveModulesLayout.Alignment alignment,
            boolean background,
            boolean textShadow
    ) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(lines, "lines");
        Objects.requireNonNull(bounds, "bounds");
        Objects.requireNonNull(alignment, "alignment");

        if (background) {
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), COLOR_BACKGROUND);
        }
        if (selected) {
            graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), COLOR_TEXT);
        }
        int textWidth = Math.max(0, bounds.width() - padding * 2);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int x = alignment == ActiveModulesLayout.Alignment.RIGHT
                    ? bounds.x() + padding + textWidth - font.width(line)
                    : bounds.x() + padding;
            graphics.text(font, line, x, bounds.y() + padding + index * font.lineHeight, textColor, textShadow);
        }
    }

    private void drawAnimated(GuiGraphicsExtractor graphics, Font font, List<String> lines, HudBounds bounds,
                              int textColor, int padding, ActiveModulesLayout.Alignment alignment,
                              boolean background, boolean textShadow, long now) {
        if (background) {
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(), COLOR_BACKGROUND);
        }
        int textWidth = Math.max(0, bounds.width() - padding * 2);
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int x = alignment == ActiveModulesLayout.Alignment.RIGHT
                    ? bounds.x() + padding + textWidth - font.width(line) : bounds.x() + padding;
            float progress = entryOpacity(visibleSince.getOrDefault(line, now), now);
            int alpha = Math.round(((textColor >>> 24) & 0xFF) * progress);
            int fadedColor = textColor & 0x00FFFFFF | alpha << 24;
            graphics.text(font, line, x, bounds.y() + padding + index * font.lineHeight, fadedColor, textShadow);
        }
    }

    static float entryOpacity(long visibleSince, long now) {
        return Math.clamp((now - visibleSince) / 150.0F, 0.0F, 1.0F);
    }
}
