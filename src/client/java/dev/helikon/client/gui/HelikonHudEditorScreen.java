package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.HudConfigurationManager;
import dev.helikon.client.hud.ActiveModules;
import dev.helikon.client.hud.ActiveModulesHud;
import dev.helikon.client.hud.ActiveModulesLayout;
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
    private static final int HEADER_BOTTOM = 130;

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
        ActiveModulesLayout style = layout.activeModules();
        graphics.pose().pushMatrix();
        graphics.pose().translate(style.x(), style.y());
        graphics.pose().scale(style.scale());
        ActiveModulesHud.draw(graphics, font, previewLines(), new HudBounds(0, 0,
                unscaledPreviewBounds().width(), unscaledPreviewBounds().height()),
                style.enabled() ? previewColor() : COLOR_DISABLED, true, style.padding(), style.alignment(),
                style.background(), style.textShadow());
        graphics.pose().popMatrix();
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
        if (isInside(mouseX, mouseY, 14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE)) {
            style.setEnabled(!style.enabled());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 48, 220, 11)) {
            style.setSort(next(style.sort()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 61, 220, 11)) {
            style.setAlignment(next(style.alignment()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 74, 220, 11)) {
            style.setColorMode(next(style.colorMode()));
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 87, 220, 11)) {
            style.setBackground(!style.background());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 100, 220, 11)) {
            style.setTextShadow(!style.textShadow());
            return true;
        }
        if (isInside(mouseX, mouseY, 14, 113, 42, 11)) {
            style.setScale(Math.max(ActiveModulesLayout.MIN_SCALE, style.scale() - 0.25F));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 59, 113, 42, 11)) {
            style.setScale(Math.min(ActiveModulesLayout.MAX_SCALE, style.scale() + 0.25F));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 104, 113, 42, 11)) {
            style.setPadding(Math.max(ActiveModulesLayout.MIN_PADDING, style.padding() - 1));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 149, 113, 42, 11)) {
            style.setPadding(Math.min(ActiveModulesLayout.MAX_PADDING, style.padding() + 1));
            state.clampToViewport(width, height, previewBounds());
            return true;
        }
        if (isInside(mouseX, mouseY, 194, 113, 42, 11)) {
            style.reset();
            state.clampToViewport(width, height, previewBounds());
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
        graphics.fill(8, 8, width - 8, HEADER_BOTTOM, COLOR_PANEL);
        graphics.outline(8, 8, width - 16, HEADER_BOTTOM - 8, COLOR_OUTLINE);
        graphics.text(font, title, 14, 14, COLOR_ACCENT, true);
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.instructions"), 14, 25, COLOR_TEXT_DIM, false);

        ActiveModulesLayout style = layout.activeModules();
        if (style.enabled()) {
            graphics.fill(14, 31, 14 + CHECKBOX_SIZE, 31 + CHECKBOX_SIZE, COLOR_ACCENT);
        } else {
            graphics.outline(14, 31, CHECKBOX_SIZE, CHECKBOX_SIZE, COLOR_TEXT_DIM);
        }
        graphics.text(font, Component.translatable("screen.helikon.hud_editor.active_modules"), 27, 31, COLOR_TEXT, false);
        graphics.text(font, "Sort: " + style.sort().name().toLowerCase(), 14, 48, COLOR_TEXT_DIM, false);
        graphics.text(font, "Alignment: " + style.alignment().name().toLowerCase(), 14, 61, COLOR_TEXT_DIM, false);
        graphics.text(font, "Color: " + style.colorMode().name().toLowerCase(), 14, 74, COLOR_TEXT_DIM, false);
        graphics.text(font, "Background: " + (style.background() ? "on" : "off"), 14, 87, COLOR_TEXT_DIM, false);
        graphics.text(font, "Text shadow: " + (style.textShadow() ? "on" : "off"), 14, 100, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale -", 14, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Scale +", 59, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad -", 104, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Pad +", 149, 113, COLOR_TEXT_DIM, false);
        graphics.text(font, "Reset", 194, 113, COLOR_ACCENT, false);
    }

    private List<String> previewLines() {
        List<String> names = ActiveModules.enabledNames(modules, layout.activeModules().sort(), font::width);
        return names.isEmpty() ? List.of("No modules enabled") : names;
    }

    private HudBounds previewBounds() {
        HudBounds raw = unscaledPreviewBounds();
        float scale = layout.activeModules().scale();
        return new HudBounds(raw.x(), raw.y(), (int) Math.ceil(raw.width() * scale), (int) Math.ceil(raw.height() * scale));
    }

    private HudBounds unscaledPreviewBounds() {
        ActiveModulesLayout style = layout.activeModules();
        return ActiveModulesHud.bounds(font, previewLines(), style.x(), style.y(), style.padding());
    }

    private int previewColor() {
        return layout.activeModules().colorMode() == ActiveModulesLayout.ColorMode.RAINBOW
                ? ActiveModules.rainbowColor(System.currentTimeMillis() / 50L)
                : ActiveModulesHud.COLOR_TEXT;
    }

    private static <E extends Enum<E>> E next(E value) {
        E[] values = value.getDeclaringClass().getEnumConstants();
        return values[(value.ordinal() + 1) % values.length];
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
