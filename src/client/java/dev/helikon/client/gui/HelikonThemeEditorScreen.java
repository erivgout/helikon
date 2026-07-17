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

/** Compact local palette selector for the ClickGUI. */
public final class HelikonThemeEditorScreen extends Screen {
    private static final int PANEL_WIDTH = 220;
    private static final int ROW_HEIGHT = 28;

    private final Screen parent;
    private final ModuleRegistry modules;
    private final ConfigurationManager configuration;
    private final ClickGuiWindowState windowState;

    private int panelX;
    private int panelY;

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
        panelY = Math.max(12, (height - (100 + ClickGuiTheme.values().length * ROW_HEIGHT)) / 2);
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
        int panelHeight = 100 + ClickGuiTheme.values().length * ROW_HEIGHT;
        graphics.fill(0, 0, width, height, 0x90000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + panelHeight, selected.panel());
        graphics.outline(panelX, panelY, PANEL_WIDTH, panelHeight, selected.outline());
        graphics.text(font, title, panelX + 8, panelY + 8, selected.accent(), true);
        graphics.text(font, Component.translatable("screen.helikon.theme_editor.instructions"),
                panelX + 8, panelY + 20, selected.textDim(), false);

        for (int index = 0; index < ClickGuiTheme.values().length; index++) {
            ClickGuiTheme theme = ClickGuiTheme.values()[index];
            int rowY = panelY + 36 + index * ROW_HEIGHT;
            boolean active = theme == selected;
            boolean hovered = isInside(mouseX, mouseY, panelX + 6, rowY, PANEL_WIDTH - 12, ROW_HEIGHT - 4);
            if (active) {
                graphics.fill(panelX + 6, rowY, panelX + PANEL_WIDTH - 6, rowY + ROW_HEIGHT - 4, theme.rowSelected());
            } else if (hovered) {
                graphics.fill(panelX + 6, rowY, panelX + PANEL_WIDTH - 6, rowY + ROW_HEIGHT - 4, selected.rowHover());
            }
            graphics.fill(panelX + 10, rowY + 6, panelX + 28, rowY + 18, theme.accent());
            graphics.text(font, theme.displayName(), panelX + 34, rowY + 7, selected.text(), false);
        }
        int controlsY = panelY + 40 + ClickGuiTheme.values().length * ROW_HEIGHT;
        graphics.text(font, "GUI scale: " + String.format(java.util.Locale.ROOT, "%.2fx", windowState.interfaceScale())
                + " (click to cycle)", panelX + 8, controlsY, selected.textDim(), false);
        graphics.text(font, "Reduced animation: " + (windowState.reducedAnimations() ? "on" : "off"),
                panelX + 8, controlsY + 14, selected.textDim(), false);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || super.mouseClicked(event, doubleClick)) {
            return event.button() == 0;
        }
        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        for (int index = 0; index < ClickGuiTheme.values().length; index++) {
            int rowY = panelY + 36 + index * ROW_HEIGHT;
            if (isInside(mouseX, mouseY, panelX + 6, rowY, PANEL_WIDTH - 12, ROW_HEIGHT - 4)) {
                windowState.setTheme(ClickGuiTheme.values()[index]);
                return true;
            }
        }
        int controlsY = panelY + 40 + ClickGuiTheme.values().length * ROW_HEIGHT;
        if (isInside(mouseX, mouseY, panelX + 6, controlsY - 2, PANEL_WIDTH - 12, 11)) {
            windowState.setInterfaceScale(nextScale(windowState.interfaceScale()));
            return true;
        }
        if (isInside(mouseX, mouseY, panelX + 6, controlsY + 12, PANEL_WIDTH - 12, 11)) {
            windowState.setReducedAnimations(!windowState.reducedAnimations());
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
