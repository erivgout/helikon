package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.ActiveModulesLayout;
import dev.helikon.client.hud.HudEditorState;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudElementPlacement;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Presentation settings for the Active Modules list and each registered HUD
 * element. Positioning happens in the separate drag-only
 * {@link HelikonHudEditorScreen}, which this screen returns to on Escape.
 */
public final class HelikonHudSettingsScreen extends Screen {
    private static final int CHECKBOX_SIZE = 8;
    private static final int PANEL_BOTTOM = 260;

    private final HelikonHudEditorScreen editor;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;
    private HudPreviewRenderer previews;

    HelikonHudSettingsScreen(
            HelikonHudEditorScreen editor,
            ModuleRegistry modules,
            HudLayout layout,
            HudConfigurationManager configuration
    ) {
        super(Component.translatable("screen.helikon.hud_settings.title"));
        this.editor = Objects.requireNonNull(editor, "editor");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.layout = Objects.requireNonNull(layout, "layout");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.state = new HudEditorState(layout);
    }

    @Override
    protected void init() {
        super.init();
        previews = new HudPreviewRenderer(font, modules, layout);
        state.clampToViewport(width, height, previews.activeModulesBounds());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractTransparentBackground(graphics);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, width, height, HudPreviewRenderer.COLOR_SCRIM);
        drawPanel(graphics);
        previews.drawActiveModules(graphics);
        previews.drawElement(graphics, editor.selectedElement(), width, height, true);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (super.mouseClicked(event, doubleClick)) {
            return true;
        }
        if (event.button() != 0) {
            return false;
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        ActiveModulesLayout style = layout.activeModules();
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            style.setEnabled(!style.enabled());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 48, 220, 11)) {
            style.setSort(next(style.sort()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 61, 220, 11)) {
            style.setAlignment(next(style.alignment()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 74, 220, 11)) {
            style.setColorMode(next(style.colorMode()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 87, 220, 11)) {
            style.setBackground(!style.background());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 194, 100, 90, 11)) {
            style.setAnimations(!style.animations());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 100, 180, 11)) {
            style.setTextShadow(!style.textShadow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 68, 113, 42, 11)) {
            style.setScale(Math.max(ActiveModulesLayout.MIN_SCALE, style.scale() - 0.25F));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 113, 113, 42, 11)) {
            style.setScale(Math.min(ActiveModulesLayout.MAX_SCALE, style.scale() + 0.25F));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 158, 113, 42, 11)) {
            style.setPadding(Math.max(ActiveModulesLayout.MIN_PADDING, style.padding() - 1));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 203, 113, 42, 11)) {
            style.setPadding(Math.min(ActiveModulesLayout.MAX_PADDING, style.padding() + 1));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 248, 113, 42, 11)) {
            style.reset();
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        HudElementId selected = editor.selectedElement();
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 126, 220, 11)) {
            editor.selectElement(next(selected));
            return true;
        }
        HudElementPlacement element = layout.element(selected);
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 139, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            element.setEnabled(!element.enabled());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 152, 220, 11)) {
            element.setAlignment(next(element.alignment()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 165, 220, 11)) {
            element.setBackground(!element.background());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 178, 220, 11)) {
            element.setTextShadow(!element.textShadow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 191, 42, 11)) {
            element.setScale(Math.max(HudElementPlacement.MIN_SCALE, element.scale() - 0.25F));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 59, 191, 42, 11)) {
            element.setScale(Math.min(HudElementPlacement.MAX_SCALE, element.scale() + 0.25F));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 104, 191, 42, 11)) {
            element.setPadding(Math.max(HudElementPlacement.MIN_PADDING, element.padding() - 1));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 149, 191, 42, 11)) {
            element.setPadding(Math.min(HudElementPlacement.MAX_PADDING, element.padding() + 1));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 204, 220, 11)) {
            element.setColor(nextColor(element.color()));
            element.setRainbow(false);
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 217, 220, 11)) {
            element.setRainbow(!element.rainbow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, mouseY, 14, 230, 220, 11)) {
            element.reset(selected);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(editor);
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(layout);
        } catch (ConfigurationException exception) {
            HelikonClient.LOGGER.log(Level.WARNING,
                    "Unable to save HUD layout while closing the HUD settings screen", exception);
        }
    }

    private void drawPanel(GuiGraphicsExtractor graphics) {
        graphics.fill(8, 8, width - 8, PANEL_BOTTOM, HudPreviewRenderer.COLOR_PANEL);
        graphics.outline(8, 8, width - 16, PANEL_BOTTOM - 8, HudPreviewRenderer.COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, HudPreviewRenderer.COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_settings.instructions"), 14, 25, HudPreviewRenderer.COLOR_TEXT_DIM, false);

        ActiveModulesLayout style = layout.activeModules();
        if (style.enabled()) {
            graphics.fill(14, 31, 14 + CHECKBOX_SIZE, 31 + CHECKBOX_SIZE, HudPreviewRenderer.COLOR_ACCENT);
        } else {
            graphics.outline(14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE, HudPreviewRenderer.COLOR_TEXT_DIM);
        }
        graphics.text(font, "Show Active Modules on HUD", 27, 31, HudPreviewRenderer.COLOR_TEXT, false);
        graphics.text(font, "Sort: " + style.sort().name().toLowerCase(), 14, 48, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Alignment: " + style.alignment().name().toLowerCase(), 14, 61, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        boolean activeRainbow = style.colorMode() == ActiveModulesLayout.ColorMode.RAINBOW;
        graphics.text(font, "Active-list rainbow: " + (activeRainbow ? "on" : "off") + " (click)",
                14, 74, activeRainbow ? HudPreviewRenderer.COLOR_ACCENT : HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Background: " + (style.background() ? "on" : "off"), 14, 87, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Text shadow: " + (style.textShadow() ? "on" : "off"), 14, 100, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Animation: " + (style.animations() ? "on" : "off"), 194, 100, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, String.format(java.util.Locale.ROOT, "Size %.2fx", style.scale()),
                14, 113, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Smaller", 68, 113, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Larger", 113, 113, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad -", 158, 113, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad +", 203, 113, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Reset", 248, 113, HudPreviewRenderer.COLOR_ACCENT, false);

        HudElementId selected = editor.selectedElement();
        HudElementPlacement element = layout.element(selected);
        graphics.text(font, "HUD element: " + HudPreviewRenderer.elementName(selected) + " (click to change)",
                14, 126, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        if (element.enabled()) {
            graphics.fill(14, 139, 14 + CHECKBOX_SIZE, 139 + CHECKBOX_SIZE, HudPreviewRenderer.COLOR_ACCENT);
        } else {
            graphics.outline(14, 139, CHECKBOX_SIZE, CHECKBOX_SIZE, HudPreviewRenderer.COLOR_TEXT_DIM);
        }
        graphics.text(font, "Show this element on the HUD", 27, 139, HudPreviewRenderer.COLOR_TEXT, false);
        graphics.text(font, "Alignment: " + element.alignment().name().toLowerCase(), 14, 152, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Background: " + (element.background() ? "on" : "off"), 14, 165, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Text shadow: " + (element.textShadow() ? "on" : "off"), 14, 178, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale -", 14, 191, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale +", 59, 191, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad -", 104, 191, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad +", 149, 191, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Color: " + colorToken(element.color()) + " (click to cycle)", 14, 204, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Rainbow: " + (element.rainbow() ? "on" : "off"), 14, 217, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        graphics.text(font, "Reset selected element", 14, 230, HudPreviewRenderer.COLOR_ACCENT, false);
    }

    private static <E extends Enum<E>> E next(E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static int nextColor(int value) {
        int[] colors = {0xFFE5EDF5, 0xFFFFD180, 0xFF80D8FF, 0xFFA5D6A7, 0xFFFF8A80};
        for (int index = 0; index < colors.length; index++) {
            if (colors[index] == value) {
                return colors[(index + 1) % colors.length];
            }
        }
        return colors[0];
    }

    private static String colorToken(int color) {
        return String.format("#%08X", color);
    }
}
