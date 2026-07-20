package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.HudBounds;
import dev.helikon.client.hud.HudEditorGrid;
import dev.helikon.client.hud.HudEditorState;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.logging.Level;

/**
 * Drag-only HUD layout editor. Every enabled element preview can be dragged
 * directly; presentation options live in the separate
 * {@link HelikonHudSettingsScreen}, reached through the header button.
 */
public final class HelikonHudEditorScreen extends Screen {
    private static final int TOOLBAR_BOTTOM = 22;
    private static final int SETTINGS_BUTTON_WIDTH = 76;
    private static final int SETTINGS_BUTTON_HEIGHT = 14;
    private static final int SETTINGS_BUTTON_Y = 4;

    private final Screen parent;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;
    private HudPreviewRenderer previews;
    private HudElementId selectedElement = HudElementId.LIVE_COORDINATES;
    private HudElementId draggedElement;
    private int elementDragOffsetX;
    private int elementDragOffsetY;

    public HelikonHudEditorScreen(
            Screen parent,
            ModuleRegistry modules,
            HudLayout layout,
            HudConfigurationManager configuration
    ) {
        super(Component.translatable("screen.helikon.hud_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
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
        keepElementsBelowToolbar();
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
        drawGrid(graphics);
        previews.drawActiveModules(graphics);
        for (HudElementId element : HudElementId.values()) {
            if (previews.activeElement(element) || element == selectedElement) {
                previews.drawElement(graphics, element, width, height, element == selectedElement);
            }
        }
        drawToolbar(graphics);
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
        if (HudPreviewRenderer.isInside(mouseX, mouseY, settingsButtonX(), SETTINGS_BUTTON_Y,
                SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT)) {
            minecraft.setScreenAndShow(new HelikonHudSettingsScreen(this, modules, layout, configuration));
            return true;
        }
        HudElementId[] elements = HudElementId.values();
        for (int index = elements.length - 1; index >= 0; index--) {
            HudElementId element = elements[index];
            if (!(previews.activeElement(element) || element == selectedElement)) {
                continue;
            }
            HudBounds bounds = previews.elementBounds(element, width, height);
            if (bounds.contains(mouseX, mouseY)) {
                selectedElement = element;
                if (!element.positionLocked()) {
                    draggedElement = element;
                    elementDragOffsetX = mouseX - bounds.x();
                    elementDragOffsetY = mouseY - bounds.y();
                }
                return true;
            }
        }
        return state.beginDrag(mouseX, mouseY, previews.activeModulesBounds());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && draggedElement != null) {
            HudBounds bounds = previews.elementBounds(draggedElement, width, height);
            int maximumX = Math.max(0, width - bounds.width());
            int maximumY = Math.max(TOOLBAR_BOTTOM, height - bounds.height());
            int x = HudEditorGrid.snap((int) event.x() - elementDragOffsetX, 0, maximumX);
            int y = HudEditorGrid.snap((int) event.y() - elementDragOffsetY,
                    Math.min(TOOLBAR_BOTTOM, maximumY), maximumY);
            layout.element(draggedElement).setAbsolutePosition(x, y);
            return true;
        }
        if (event.button() == 0
                && state.dragTo((int) event.x(), (int) event.y(), width, height,
                TOOLBAR_BOTTOM, previews.activeModulesBounds())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && draggedElement != null) {
            draggedElement = null;
            return true;
        }
        if (event.button() == 0 && state.isDragging()) {
            state.endDrag();
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(layout);
        } catch (ConfigurationException exception) {
            HelikonClient.LOGGER.log(Level.WARNING, "Unable to save HUD layout while closing the HUD editor", exception);
        }
    }

    /** Element whose settings the HUD settings screen opens on. */
    HudElementId selectedElement() {
        return selectedElement;
    }

    void selectElement(HudElementId element) {
        selectedElement = Objects.requireNonNull(element, "element");
    }

    private void drawToolbar(GuiGraphicsExtractor graphics) {
        graphics.fill(0, 0, width, TOOLBAR_BOTTOM, HudPreviewRenderer.COLOR_PANEL);
        graphics.fill(0, TOOLBAR_BOTTOM - 1, width, TOOLBAR_BOTTOM, HudPreviewRenderer.COLOR_OUTLINE);
        graphics.text(font, "HUD Editor", 7, 7, HudPreviewRenderer.COLOR_ACCENT, true);
        String selected = "Selected: " + HudPreviewRenderer.elementName(selectedElement);
        if (width - SETTINGS_BUTTON_WIDTH > 210) {
            graphics.text(font, selected, 82, 7, HudPreviewRenderer.COLOR_TEXT_DIM, false);
        }
        Component label = Component.translatable("screen.helikon.hud_editor.settings_button");
        int buttonX = settingsButtonX();
        graphics.outline(buttonX, SETTINGS_BUTTON_Y, SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT,
                HudPreviewRenderer.COLOR_ACCENT);
        graphics.text(font, label, buttonX + (SETTINGS_BUTTON_WIDTH - font.width(label)) / 2,
                SETTINGS_BUTTON_Y + (SETTINGS_BUTTON_HEIGHT - font.lineHeight) / 2 + 1,
                HudPreviewRenderer.COLOR_TEXT, false);
    }

    private int settingsButtonX() {
        return width - 7 - SETTINGS_BUTTON_WIDTH;
    }

    private void drawGrid(GuiGraphicsExtractor graphics) {
        for (int x = 0; x < width; x += HudEditorGrid.SIZE) {
            graphics.fill(x, TOOLBAR_BOTTOM, x + 1, height, HudPreviewRenderer.COLOR_GRID);
        }
        for (int y = TOOLBAR_BOTTOM; y < height; y += HudEditorGrid.SIZE) {
            graphics.fill(0, y, width, y + 1, HudPreviewRenderer.COLOR_GRID);
        }
    }

    private void keepElementsBelowToolbar() {
        HudBounds activeBounds = previews.activeModulesBounds();
        if (activeBounds.y() < TOOLBAR_BOTTOM) {
            layout.setActiveModulesPosition(activeBounds.x(), TOOLBAR_BOTTOM);
        }
        for (HudElementId element : HudElementId.values()) {
            if (element.positionLocked() || !(previews.activeElement(element) || element == selectedElement)) {
                continue;
            }
            HudBounds bounds = previews.elementBounds(element, width, height);
            if (bounds.y() < TOOLBAR_BOTTOM) {
                layout.element(element).setAbsolutePosition(bounds.x(), TOOLBAR_BOTTOM);
            }
        }
    }
}
