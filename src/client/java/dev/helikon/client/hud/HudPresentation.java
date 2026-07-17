package dev.helikon.client.hud;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/** Shared bounded presentation for text-based HUD elements. */
public final class HudPresentation {
    private static final int BACKGROUND = 0xB014161B;

    private HudPresentation() {
    }

    /** Draws a local text block with every persisted per-element presentation option. */
    public static HudBounds drawLines(GuiGraphicsExtractor graphics, Font font, List<String> lines,
                                      HudElementPlacement placement) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(font, "font");
        lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
        Objects.requireNonNull(placement, "placement");
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("HUD text lines cannot be empty");
        }

        int textWidth = lines.stream().mapToInt(font::width).max().orElse(0);
        int contentWidth = textWidth + placement.padding() * 2;
        int contentHeight = lines.size() * font.lineHeight + placement.padding() * 2;
        HudBounds bounds = placement.scaledBounds(graphics.guiWidth(), graphics.guiHeight(), contentWidth, contentHeight);
        int color = color(placement);

        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x(), bounds.y());
        graphics.pose().scale(placement.scale());
        if (placement.background()) {
            graphics.fill(0, 0, contentWidth, contentHeight, BACKGROUND);
        }
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            int textX = switch (placement.alignment()) {
                case LEFT -> placement.padding();
                case CENTER -> (contentWidth - font.width(line)) / 2;
                case RIGHT -> contentWidth - placement.padding() - font.width(line);
            };
            graphics.text(font, line, textX, placement.padding() + index * font.lineHeight, color, placement.textShadow());
        }
        graphics.pose().popMatrix();
        return bounds;
    }

    /** Resolves the selected solid or local animated color for a HUD element. */
    public static int color(HudElementPlacement placement) {
        Objects.requireNonNull(placement, "placement");
        return placement.rainbow() ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L) : placement.color();
    }

    /** Opens a scaled, padded local frame for HUD renderers that are not text blocks. */
    public static Frame beginFrame(GuiGraphicsExtractor graphics, HudElementPlacement placement,
                                   int contentWidth, int contentHeight) {
        return beginFrame(graphics, placement, contentWidth, contentHeight, true);
    }

    /** Opens a placement frame without drawing the generic background or outline. */
    public static Frame beginTransparentFrame(GuiGraphicsExtractor graphics, HudElementPlacement placement,
                                              int contentWidth, int contentHeight) {
        return beginFrame(graphics, placement, contentWidth, contentHeight, false);
    }

    private static Frame beginFrame(GuiGraphicsExtractor graphics, HudElementPlacement placement,
                                    int contentWidth, int contentHeight, boolean decorate) {
        Objects.requireNonNull(graphics, "graphics");
        Objects.requireNonNull(placement, "placement");
        if (contentWidth < 0 || contentHeight < 0) {
            throw new IllegalArgumentException("HUD dimensions cannot be negative");
        }
        int outerWidth = contentWidth + placement.padding() * 2;
        int outerHeight = contentHeight + placement.padding() * 2;
        HudBounds bounds = placement.scaledBounds(graphics.guiWidth(), graphics.guiHeight(), outerWidth, outerHeight);
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x(), bounds.y());
        graphics.pose().scale(placement.scale());
        if (decorate && placement.background()) {
            graphics.fill(0, 0, outerWidth, outerHeight, BACKGROUND);
        }
        if (decorate && placement.textShadow()) {
            graphics.outline(0, 0, outerWidth, outerHeight, color(placement));
        }
        int horizontalOffset = switch (placement.alignment()) {
            case LEFT -> 0;
            case CENTER -> placement.padding() / 2;
            case RIGHT -> placement.padding();
        };
        return new Frame(bounds, horizontalOffset + placement.padding(), placement.padding());
    }

    /** A caller must invoke {@link #endFrame(GuiGraphicsExtractor)} after rendering inside this frame. */
    public record Frame(HudBounds bounds, int contentX, int contentY) {
    }

    public static void endFrame(GuiGraphicsExtractor graphics) {
        graphics.pose().popMatrix();
    }
}
