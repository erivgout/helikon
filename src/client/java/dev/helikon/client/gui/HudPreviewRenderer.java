package dev.helikon.client.gui;

import dev.helikon.client.hud.ActiveModules;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.ActiveModulesLayout;
import dev.helikon.client.hud.HudBounds;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudElementPlacement;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.render.MiniPlayerLayout;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/**
 * Shared preview drawing for the HUD editor and HUD settings screens: the
 * Active Modules list plus one placement handle per registered HUD element.
 */
final class HudPreviewRenderer {
    static final int COLOR_SCRIM = 0xF0101319;
    static final int COLOR_GRID = 0xFF1C222B;
    static final int COLOR_PANEL = 0xEE14171D;
    static final int COLOR_OUTLINE = 0xFF2A2F38;
    static final int COLOR_ACCENT = 0xFFE8A33D;
    static final int COLOR_TEXT = 0xFFE6E6E6;
    static final int COLOR_TEXT_DIM = 0xFF9AA1AB;
    static final int COLOR_DISABLED = 0xFF777D86;

    private final Font font;
    private final ModuleRegistry modules;
    private final HudLayout layout;

    HudPreviewRenderer(Font font, ModuleRegistry modules, HudLayout layout) {
        this.font = Objects.requireNonNull(font, "font");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    /** Scaled Active Modules footprint used for dragging and clamping. */
    HudBounds activeModulesBounds() {
        HudBounds raw = unscaledActiveModulesBounds(activeModulesLines());
        float scale = layout.activeModules().scale();
        return new HudBounds(raw.x(), raw.y(), (int) Math.ceil(raw.width() * scale), (int) Math.ceil(raw.height() * scale));
    }

    void drawActiveModules(GuiGraphicsExtractor graphics) {
        ActiveModulesLayout style = layout.activeModules();
        List<String> lines = activeModulesLines();
        HudBounds raw = unscaledActiveModulesBounds(lines);
        graphics.pose().pushMatrix();
        graphics.pose().translate(style.x(), style.y());
        graphics.pose().scale(style.scale());
        ActiveModulesHud.draw(graphics, font, lines, new HudBounds(0, 0, raw.width(), raw.height()),
                style.enabled() ? activeModulesColor() : COLOR_DISABLED, true, style.padding(), style.alignment(),
                style.background(), style.textShadow(),
                style.enabled() && style.colorMode() == ActiveModulesLayout.ColorMode.RAINBOW,
                System.currentTimeMillis() / 50L);
        graphics.pose().popMatrix();
    }

    /** Scaled placement-handle footprint for one registered HUD element. */
    HudBounds elementBounds(HudElementId element, int viewportWidth, int viewportHeight) {
        HudElementPlacement placement = layout.element(element);
        PreviewSize size = previewSize(element, placement);
        return placement.scaledBounds(viewportWidth, viewportHeight, size.width(), size.height());
    }

    void drawElement(GuiGraphicsExtractor graphics, HudElementId element, int viewportWidth, int viewportHeight,
                     boolean selected) {
        HudElementPlacement placement = layout.element(element);
        String name = elementName(element);
        int nameWidth = font.width(name);
        PreviewSize size = previewSize(element, placement);
        HudBounds bounds = placement.scaledBounds(viewportWidth, viewportHeight, size.width(), size.height());
        int width = size.width();
        int height = size.height();
        int color = placement.enabled() ? (placement.rainbow()
                ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L) : placement.color()) : COLOR_DISABLED;
        graphics.pose().pushMatrix();
        graphics.pose().translate(bounds.x(), bounds.y());
        graphics.pose().scale(placement.scale());
        if (placement.background()) {
            graphics.fill(0, 0, width, height, COLOR_PANEL);
        }
        graphics.outline(0, 0, width, height, selected ? COLOR_ACCENT : COLOR_OUTLINE);
        int textX = switch (placement.alignment()) {
            case LEFT -> placement.padding();
            case CENTER -> (width - nameWidth) / 2;
            case RIGHT -> width - placement.padding() - nameWidth;
        };
        graphics.text(font, name, textX, placement.padding(), color, placement.textShadow());
        graphics.pose().popMatrix();
    }

    /** Keeps module-backed handles off the canvas until their module is enabled or the handle is selected. */
    boolean activeElement(HudElementId element) {
        if (!layout.element(element).enabled()) {
            return false;
        }
        return switch (element) {
            case LIVE_COORDINATES -> enabled("coordinates");
            case KEYSTROKES -> enabled("keystrokes");
            case TIME -> enabled("time");
            case SATURATION -> enabled("saturation_display");
            case ELYTRA -> enabled("extra_elytra");
            case TARGET -> enabled("target_hud");
            case REACH -> enabled("reach_display");
            case INVENTORY_PREVIEW -> enabled("inventory_preview");
            case DURABILITY_WARNINGS -> enabled("durability_warnings");
            case RADAR -> enabled("radar");
            case MINI_PLAYER -> enabled("mini_player");
            case DEBUG_OVERLAY -> enabled("debug_overlay");
            case BETTER_CROSSHAIR -> enabled("better_crosshair");
            case HEALTH -> enabled("health");
            case COORDINATES -> enabled("death_coordinates") || enabled("logout_coordinates");
            default -> true;
        };
    }

    static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    static String elementName(HudElementId element) {
        return switch (element) {
            case COORDINATES -> "Saved Coordinates";
            case LIVE_COORDINATES -> "Coordinates";
            case KEYSTROKES -> "Keystrokes";
            case TIME -> "Time";
            case SATURATION -> "Saturation";
            case ELYTRA -> "Elytra";
            case TARGET -> "Target HUD";
            case REACH -> "Reach";
            case INVENTORY_PREVIEW -> "Inventory Preview";
            case DURABILITY_WARNINGS -> "Durability warnings";
            case RADAR -> "Radar";
            case MINI_PLAYER -> "Mini Player";
            case DEBUG_OVERLAY -> "Debug Overlay";
            case BETTER_CROSSHAIR -> "Better Crosshair";
            case HEALTH -> "Health";
            case DIRECTION -> "Direction";
            case FPS -> "FPS";
            case PING -> "Ping";
            case TPS -> "TPS estimate";
            case SPEED -> "Speed";
            case ARMOR_DURABILITY -> "Armor durability";
            case HELD_ITEM_DURABILITY -> "Held-item durability";
            case POTION_EFFECTS -> "Potion effects";
            case CLOCK -> "Clock";
            case BIOME -> "Biome";
            case SERVER_ADDRESS -> "Server address";
            case TOTEM_COUNT -> "Totem count";
        };
    }

    private List<String> activeModulesLines() {
        List<String> names = ActiveModules.enabledNames(modules, layout.activeModules().sort(), font::width);
        return names.isEmpty() ? List.of("No modules enabled") : names;
    }

    private HudBounds unscaledActiveModulesBounds(List<String> lines) {
        ActiveModulesLayout style = layout.activeModules();
        return ActiveModulesHud.bounds(font, lines, style.x(), style.y(), style.padding());
    }

    private PreviewSize previewSize(HudElementId element, HudElementPlacement placement) {
        if (element == HudElementId.MINI_PLAYER) {
            return new PreviewSize(MiniPlayerLayout.WIDTH + placement.padding() * 2,
                    MiniPlayerLayout.HEIGHT + placement.padding() * 2);
        }
        return new PreviewSize(font.width(elementName(element)) + placement.padding() * 2,
                font.lineHeight + placement.padding() * 2);
    }

    private boolean enabled(String moduleId) {
        return modules.find(moduleId).map(dev.helikon.client.module.Module::isEnabled).orElse(false);
    }

    private int activeModulesColor() {
        return layout.activeModules().colorMode() == ActiveModulesLayout.ColorMode.RAINBOW
                ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L)
                : ActiveModulesHud.COLOR_TEXT;
    }

    private record PreviewSize(int width, int height) {
    }
}
