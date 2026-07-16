package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.ActiveModules;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.HudBounds;
import dev.helikon.client.hud.HudEditorState;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Objects;
import java.util.logging.Level;

/**
 * Minimal HUD editor for the Active Modules element. It deliberately focuses
 * on visibility and bounded drag placement; per-element styling and snapping
 * are added with the later HUD-editor expansion.
 */
public final class HelikonHudEditorScreen extends Screen {
    private static final int COLOR_SCRIM = 0x90000000;
    private static final int COLOR_PANEL = 0xEE14171D;
    private static final int COLOR_OUTLINE = 0xFF2A2F38;
    private static final int COLOR_TEXT = 0xFFE6E6E6;
    private static final int COLOR_TEXT_DIM = 0xFF9AA1AB;
    private static final int COLOR_ACCENT = 0xFFE8A33D;
    private static final int COLOR_DISABLED = 0xFF777D86;
    private static final int CHECKBOX_SIZE = 8;

    private final Screen parent;
    private final ModuleRegistry modules;
    private final HudLayout layout;
    private final HudConfigurationManager configuration;
    private final HudEditorState state;

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
        state.clampToViewport(width, height, previewBounds());
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
        graphics.fill(0, 0, width, height, COLOR_SCRIM);
        drawHeader(graphics);

        HudBounds bounds = previewBounds();
        ActiveModulesHud.draw(graphics, font, previewLines(), bounds,
                layout.activeModulesEnabled() ? ActiveModulesHud.COLOR_TEXT : COLOR_DISABLED, true);
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
        if (isInside(mouseX, mouseY, 14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            layout.setActiveModulesEnabled(!layout.activeModulesEnabled());
            return true;
        }
        return state.beginDrag(mouseX, mouseY, previewBounds());
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() == 0 && state.dragTo((int) event.x(), (int) event.y(), width, height, previewBounds())) {
            return true;
        }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
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

    private void drawHeader(GuiGraphicsExtractor graphics) {
        int headerBottom = 48;
        graphics.fill(8, 8, width - 8, headerBottom, COLOR_PANEL);
        graphics.outline(8, 8, width - 16, headerBottom - 8, COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.instructions"), 14, 25, COLOR_TEXT_DIM, false);

        if (layout.activeModulesEnabled()) {
            graphics.fill(14, 31, 14 + CHECKBOX_SIZE, 31 + CHECKBOX_SIZE, COLOR_ACCENT);
        } else {
            graphics.outline(14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_TEXT_DIM);
        }
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.active_modules"), 27, 31, COLOR_TEXT, false);
    }

    private List<String> previewLines() {
        List<String> names = ActiveModules.enabledNames(modules);
        return names.isEmpty() ? List.of("No modules enabled") : names;
    }

    private HudBounds previewBounds() {
        return ActiveModulesHud.bounds(font, previewLines(), layout.activeModulesX(), layout.activeModulesY());
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
