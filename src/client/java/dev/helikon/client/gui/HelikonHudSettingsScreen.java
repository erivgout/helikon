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
    private static final int PANEL_MAX_BOTTOM = 260;
    private static final int PANEL_MARGIN = 8;
    private static final int CONTENT_TOP = 31;
    private static final int CONTENT_BOTTOM = 242;
    private static final int CONTENT_HEIGHT = CONTENT_BOTTOM - CONTENT_TOP;
    private static final int SCROLLBAR_WIDTH = 4;
    private static final double SCROLL_STEP = 18.0D;

    private final HelikonHudEditorScreen editor;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;
    private final ClickGuiScrollbarState scrollbar = new ClickGuiScrollbarState();
    private HudPreviewRenderer previews;
    private double scrollOffset;

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
        scrollOffset = boundedScroll(scrollOffset);
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
        if (HudPreviewRenderer.isInside(mouseX, mouseY, scrollbarX(), CONTENT_TOP,
                SCROLLBAR_WIDTH, Math.max(0, panelBottom() - CONTENT_TOP))) {
            scrollbar.beginDrag(mouseY, CONTENT_TOP, panelBottom(), CONTENT_HEIGHT, scrollOffset)
                    .ifPresent(value -> scrollOffset = boundedScroll(value));
            return maxScroll() > 0.0D;
        }
        if (!insideContent(mouseX, mouseY)) {
            return false;
        }
        int contentMouseY = mouseY + (int) Math.round(scrollOffset);
        ActiveModulesLayout style = layout.activeModules();
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            style.setEnabled(!style.enabled());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 48, 220, 11)) {
            style.setSort(next(style.sort()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 61, 220, 11)) {
            style.setAlignment(next(style.alignment()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 74, 220, 11)) {
            style.setColorMode(next(style.colorMode()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 87, 220, 11)) {
            style.setBackground(!style.background());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 194, 100, 90, 11)) {
            style.setAnimations(!style.animations());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 100, 180, 11)) {
            style.setTextShadow(!style.textShadow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 68, 113, 42, 11)) {
            style.setScale(Math.max(ActiveModulesLayout.MIN_SCALE, style.scale() - 0.25F));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 113, 113, 42, 11)) {
            style.setScale(Math.min(ActiveModulesLayout.MAX_SCALE, style.scale() + 0.25F));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 158, 113, 42, 11)) {
            style.setPadding(Math.max(ActiveModulesLayout.MIN_PADDING, style.padding() - 1));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 203, 113, 42, 11)) {
            style.setPadding(Math.min(ActiveModulesLayout.MAX_PADDING, style.padding() + 1));
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 248, 113, 42, 11)) {
            style.reset();
            state.clampToViewport(width, height, previews.activeModulesBounds());
            return true;
        }
        HudElementId selected = editor.selectedElement();
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 126, 220, 11)) {
            editor.selectElement(next(selected));
            return true;
        }
        HudElementPlacement element = layout.element(selected);
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 139, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            element.setEnabled(!element.enabled());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 152, 220, 11)) {
            element.setAlignment(next(element.alignment()));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 165, 220, 11)) {
            element.setBackground(!element.background());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 178, 220, 11)) {
            element.setTextShadow(!element.textShadow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 191, 42, 11)) {
            element.setScale(Math.max(HudElementPlacement.MIN_SCALE, element.scale() - 0.25F));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 59, 191, 42, 11)) {
            element.setScale(Math.min(HudElementPlacement.MAX_SCALE, element.scale() + 0.25F));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 104, 191, 42, 11)) {
            element.setPadding(Math.max(HudElementPlacement.MIN_PADDING, element.padding() - 1));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 149, 191, 42, 11)) {
            element.setPadding(Math.min(HudElementPlacement.MAX_PADDING, element.padding() + 1));
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 204, 220, 11)) {
            element.setColor(nextColor(element.color()));
            element.setRainbow(false);
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 217, 220, 11)) {
            element.setRainbow(!element.rainbow());
            return true;
        }
        if (HudPreviewRenderer.isInside(mouseX, contentMouseY, 14, 230, 220, 11)) {
            element.reset(selected);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && scrollbar.isDragging()) {
            scrollbar.dragTo((int) event.y(), CONTENT_TOP, panelBottom(), CONTENT_HEIGHT)
                    .ifPresent(value -> scrollOffset = boundedScroll(value));
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && scrollbar.isDragging()) {
            scrollbar.endDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            return true;
        }
        if (!insideContent((int) mouseX, (int) mouseY) || vertical == 0.0D || maxScroll() <= 0.0D) {
            return false;
        }
        scrollOffset = boundedScroll(scrollOffset - vertical * SCROLL_STEP);
        return true;
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
        int panelBottom = panelBottom();
        graphics.fill(PANEL_MARGIN, PANEL_MARGIN, width - PANEL_MARGIN, panelBottom, HudPreviewRenderer.COLOR_PANEL);
        graphics.outline(PANEL_MARGIN, PANEL_MARGIN, width - PANEL_MARGIN * 2,
                panelBottom - PANEL_MARGIN, HudPreviewRenderer.COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, HudPreviewRenderer.COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_settings.instructions"), 14, 25, HudPreviewRenderer.COLOR_TEXT_DIM, false);

        if (panelBottom <= CONTENT_TOP) {
            return;
        }
        graphics.enableScissor(PANEL_MARGIN + 1, CONTENT_TOP, width - PANEL_MARGIN - 1, panelBottom - 1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(0.0F, (float) -scrollOffset);
        drawPanelContent(graphics);
        graphics.pose().popMatrix();
        graphics.disableScissor();
        drawScrollbar(graphics);
    }

    private void drawPanelContent(GuiGraphicsExtractor graphics) {
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

    private void drawScrollbar(GuiGraphicsExtractor graphics) {
        ClickGuiScrollbarState.thumb(CONTENT_TOP, panelBottom(), CONTENT_HEIGHT, scrollOffset).ifPresent(thumb -> {
            int x = scrollbarX();
            graphics.fill(x, CONTENT_TOP, x + SCROLLBAR_WIDTH, panelBottom(), HudPreviewRenderer.COLOR_OUTLINE);
            graphics.fill(x, thumb.y(), x + SCROLLBAR_WIDTH, thumb.y() + thumb.height(),
                    HudPreviewRenderer.COLOR_ACCENT);
        });
    }

    private int panelBottom() {
        return Math.max(CONTENT_TOP, Math.min(PANEL_MAX_BOTTOM, height - PANEL_MARGIN));
    }

    private int scrollbarX() {
        return Math.max(PANEL_MARGIN + 1, width - PANEL_MARGIN - SCROLLBAR_WIDTH - 1);
    }

    private boolean insideContent(int mouseX, int mouseY) {
        return HudPreviewRenderer.isInside(mouseX, mouseY, PANEL_MARGIN + 1, CONTENT_TOP,
                Math.max(0, width - PANEL_MARGIN * 2 - 2), Math.max(0, panelBottom() - CONTENT_TOP));
    }

    private double maxScroll() {
        return Math.max(0.0D, CONTENT_HEIGHT - (panelBottom() - CONTENT_TOP));
    }

    private double boundedScroll(double value) {
        return Math.clamp(value, 0.0D, maxScroll());
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
