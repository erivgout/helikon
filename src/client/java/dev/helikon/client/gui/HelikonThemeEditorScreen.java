package dev.helikon.client.gui;

import dev.helikon.client.HelikonClient;
import dev.helikon.client.config.ConfigurationException;
import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.module.ModuleRegistry;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.logging.Level;

/** Compact local palette selector for the ClickGUI with a scrollable theme list. */
public final class HelikonThemeEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 240;
    private static final int ROW_HEIGHT = 24;
    private static final int CONTROL_ROW_HEIGHT = 14;

    private final Screen parent;
    private final ModuleRegistry modules;
    private final ConfigurationManager configuration;
    private final ClickGuiWindowState windowState;

    private int panelX;
    private int panelY;
    private int panelHeight;
    private double scroll;

    public HelikonThemeEditorScreen(Screen parent, ModuleRegistry modules, ConfigurationManager configuration,
                                   ClickGuiWindowState windowState) {
        super(Component.translatable("screen.helikon.theme_editor.title"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.windowState = Objects.requireNonNull(windowState, "windowState");
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = 12;
        panelHeight = Math.min(height - 24, headerBlockHeight() + contentHeight() + 8);
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
        ClickGuiTheme selected = windowState.theme();
        scroll = clampScroll(scroll);
        graphics.fill(0, 0, width, height, 0x90000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, selected.panel());
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, selected.outline());
        graphics.text(font, HelikonUiFont.ui(title), panelX + 8, panelY + 8, selected.accent(), false);
        graphics.textWithWordWrap(font,
                HelikonUiFont.ui(Component.translatable("screen.helikon.theme_editor.instructions")),
                panelX + 8, panelY + 20, PANEL_WIDTH - 16, selected.textDim());

        int contentTop = contentTop();
        int contentBottom = contentBottom();
        graphics.enableScissor(panelX, contentTop, panelX + PANEL_WIDTH, contentBottom);
        ClickGuiTheme[] themes = ClickGuiTheme.values();
        for (int index = 0; index < themes.length; index++) {
            ClickGuiTheme theme = themes[index];
            int rowY = contentTop + index * ROW_HEIGHT - (int) scroll;
            if (rowY + ROW_HEIGHT < contentTop || rowY > contentBottom) {
                continue;
            }
            boolean active = theme == selected;
            boolean hovered = isRowHovered(mouseX, mouseY, rowY);
            if (active) {
                graphics.fill(panelX + 6, rowY, panelX + PANEL_WIDTH - 6, rowY + ROW_HEIGHT - 4,
                        theme.rowSelected());
            } else if (hovered) {
                graphics.fill(panelX + 6, rowY, panelX + PANEL_WIDTH - 6, rowY + ROW_HEIGHT - 4,
                        selected.rowHover());
            }
            graphics.fill(panelX + 10, rowY + 4, panelX + 28, rowY + ROW_HEIGHT - 8, theme.accent());
            graphics.text(font, HelikonUiFont.ui(theme.displayName()), panelX + 34, rowY + 6,
                    selected.text(), false);
        }
        int controlsY = controlsTop() - (int) scroll;
        graphics.text(font, HelikonUiFont.ui("GUI scale: "
                        + String.format(java.util.Locale.ROOT, "%.2fx", windowState.interfaceScale())
                        + " (click to cycle)"),
                panelX + 8, controlsY, selected.textDim(), false);
        graphics.text(font, HelikonUiFont.ui("Reduced animation: "
                        + (windowState.reducedAnimations() ? "on" : "off")),
                panelX + 8, controlsY + CONTROL_ROW_HEIGHT, selected.textDim(), false);
        graphics.disableScissor();

        ClickGuiScrollbarState.thumb(contentTop, contentBottom, contentHeight(), scroll).ifPresent(thumb ->
                graphics.fill(panelX + PANEL_WIDTH - 3, thumb.y(), panelX + PANEL_WIDTH - 2,
                        thumb.y() + thumb.height(), selected.accent()));
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || super.mouseClicked(event, doubleClick)) {
            return event.button() == 0;
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        ClickGuiTheme[] themes = ClickGuiTheme.values();
        for (int index = 0; index < themes.length; index++) {
            int rowY = contentTop() + index * ROW_HEIGHT - (int) scroll;
            if (isRowHovered(mouseX, mouseY, rowY)) {
                windowState.setTheme(themes[index]);
                return true;
            }
        }
        int controlsY = controlsTop() - (int) scroll;
        if (isControlHovered(mouseX, mouseY, controlsY)) {
            windowState.setInterfaceScale(nextScale(windowState.interfaceScale()));
            return true;
        }
        if (isControlHovered(mouseX, mouseY, controlsY + CONTROL_ROW_HEIGHT)) {
            windowState.setReducedAnimations(!windowState.reducedAnimations());
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (super.mouseScrolled(mouseX, mouseY, horizontal, vertical)) {
            return true;
        }
        if (isInside((int) mouseX, (int) mouseY, panelX, panelY, PANEL_WIDTH, panelHeight)) {
            scroll = clampScroll(scroll - vertical * ROW_HEIGHT);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        minecraft.setScreenAndShow(parent);
    }

    @Override
    public void removed() {
        super.removed();
        try {
            configuration.save(modules, windowState);
        } catch (ConfigurationException exception) {
            HelikonClient.LOGGER.log(Level.WARNING, "Unable to save ClickGUI theme while closing the theme editor", exception);
        }
    }

    private boolean isRowHovered(int mouseX, int mouseY, int rowY) {
        return mouseY >= contentTop() && mouseY < contentBottom()
                && isInside(mouseX, mouseY, panelX + 6, rowY, PANEL_WIDTH - 12, ROW_HEIGHT - 4);
    }

    private boolean isControlHovered(int mouseX, int mouseY, int controlY) {
        return mouseY >= contentTop() && mouseY < contentBottom()
                && isInside(mouseX, mouseY, panelX + 6, controlY - 2, PANEL_WIDTH - 12, CONTROL_ROW_HEIGHT - 2);
    }

    private int instructionsHeight() {
        return font.wordWrapHeight(
                HelikonUiFont.ui(Component.translatable("screen.helikon.theme_editor.instructions")),
                PANEL_WIDTH - 16);
    }

    private int headerBlockHeight() {
        return 20 + instructionsHeight() + 6;
    }

    private int contentHeight() {
        return ClickGuiTheme.values().length * ROW_HEIGHT + 6 + 2 * CONTROL_ROW_HEIGHT;
    }

    private int contentTop() {
        return panelY + headerBlockHeight();
    }

    private int contentBottom() {
        return panelY + panelHeight - 6;
    }

    private int controlsTop() {
        return contentTop() + ClickGuiTheme.values().length * ROW_HEIGHT + 6;
    }

    private double clampScroll(double value) {
        return Math.clamp(value, 0.0D, Math.max(0, contentHeight() - (contentBottom() - contentTop())));
    }

    private static boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    private static float nextScale(float value) {
        float[] values = {0.75F, 1.0F, 1.25F, 1.5F};
        for (int index = 0; index < values.length; index++) {
            if (Float.compare(values[index], value) == 0) {
                return values[(index + 1) % values.length];
            }
        }
        return 1.0F;
    }
}
