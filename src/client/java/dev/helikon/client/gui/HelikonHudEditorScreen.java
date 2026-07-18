package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.HudBounds;
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
    private static final int HINT_BAR_BOTTOM = 40;
    private static final int SETTINGS_BUTTON_WIDTH = 88;
    private static final int SETTINGS_BUTTON_HEIGHT = 14;
    private static final int SETTINGS_BUTTON_Y = 13;

    private final Screen parent;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;
    private HudPreviewRenderer previews;
    private HudElementId selectedElement = HudElementId.WAYPOINTS;
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
        previews.drawActiveModules(graphics);
        for (HudElementId element : HudElementId.values()) {
            if (layout.element(element).enabled() || element == selectedElement) {
                previews.drawElement(graphics, element, width, height, element == selectedElement);
            }
        }
        drawHintBar(graphics);
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
            if (!layout.element(element).enabled()) {
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
            int x = Math.clamp((int) event.x() - elementDragOffsetX, 0, Math.max(0, width - bounds.width()));
            int y = Math.clamp((int) event.y() - elementDragOffsetY, 0, Math.max(0, height - bounds.height()));
            layout.element(draggedElement).setAbsolutePosition(x, y);
            return true;
        }
        if (event.button() == 0
                && state.dragTo((int) event.x(), (int) event.y(), width, height, previews.activeModulesBounds())) {
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

    private void drawHintBar(GuiGraphicsExtractor graphics) {
        graphics.fill(8, 8, width - 8, HINT_BAR_BOTTOM, HudPreviewRenderer.COLOR_PANEL);
        graphics.outline(8, 8, width - 16, HINT_BAR_BOTTOM - 8, HudPreviewRenderer.COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, HudPreviewRenderer.COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.instructions"), 14, 25,
                HudPreviewRenderer.COLOR_TEXT_DIM, false);
        Component label = Component.translatable("screen.helikon.hud_editor.settings_button");
        int buttonX = settingsButtonX();
        graphics.outline(buttonX, SETTINGS_BUTTON_Y, SETTINGS_BUTTON_WIDTH, SETTINGS_BUTTON_HEIGHT,
                HudPreviewRenderer.COLOR_ACCENT);
        graphics.text(font, label, buttonX + (SETTINGS_BUTTON_WIDTH - font.width(label)) / 2,
                SETTINGS_BUTTON_Y + (SETTINGS_BUTTON_HEIGHT - font.lineHeight) / 2 + 1,
                HudPreviewRenderer.COLOR_TEXT, false);
    }

    private int settingsButtonX() {
        return width - 14 - SETTINGS_BUTTON_WIDTH;
    }
}
