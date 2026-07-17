package dev.helikon.client.gui;

import dev.helikon.client.module.player.AutoReconnect;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.Objects;
import java.util.function.LongSupplier;

/** Small local reconnect countdown overlay with an explicit cancel button. */
public final class HelikonAutoReconnectScreen extends Screen {
    private static final int PANEL_WIDTH = 236;
    private static final int PANEL_HEIGHT = 82;
    private static final int BUTTON_WIDTH = 96;
    private static final int BUTTON_HEIGHT = 20;

    private final Screen parent;
    private final AutoReconnect reconnect;
    private final LongSupplier tickSource;
    private int panelX;
    private int panelY;

    public HelikonAutoReconnectScreen(Screen parent, AutoReconnect reconnect, LongSupplier tickSource) {
        super(Component.literal("Helikon reconnect"));
        this.parent = Objects.requireNonNull(parent, "parent");
        this.reconnect = Objects.requireNonNull(reconnect, "reconnect");
        this.tickSource = Objects.requireNonNull(tickSource, "tickSource");
    }

    @Override
    protected void init() {
        panelX = (width - PANEL_WIDTH) / 2;
        panelY = (height - PANEL_HEIGHT) / 2;
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
        int buttonX = panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2;
        int buttonY = panelY + 54;
        boolean hoveringCancel = inside(mouseX, mouseY, buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
        graphics.fill(0, 0, width, height, 0x90000000);
        graphics.fill(panelX, panelY, panelX + PANEL_WIDTH, panelY + PANEL_HEIGHT, 0xEE20242B);
        graphics.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0xFFB9C3D0);
        graphics.centeredText(font, Component.literal("Helikon reconnect"), panelX + PANEL_WIDTH / 2, panelY + 12,
                0xFFF2F5F9);
        graphics.centeredText(font, Component.literal("Reconnecting in " + reconnect.remainingSeconds(tickSource.getAsLong())
                + "s (attempt " + (reconnect.attempts() + 1) + ")"), panelX + PANEL_WIDTH / 2, panelY + 31, 0xFFB9C3D0);
        graphics.fill(buttonX, buttonY, buttonX + BUTTON_WIDTH, buttonY + BUTTON_HEIGHT,
                hoveringCancel ? 0xFF8D3838 : 0xFF633030);
        graphics.outline(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 0xFFFFB8B8);
        graphics.centeredText(font, Component.literal("Cancel"), buttonX + BUTTON_WIDTH / 2, buttonY + 6, 0xFFFFFFFF);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && inside((int) event.x(), (int) event.y(),
                panelX + (PANEL_WIDTH - BUTTON_WIDTH) / 2, panelY + 54, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            cancel();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public void onClose() {
        cancel();
    }

    private void cancel() {
        reconnect.cancel();
        minecraft.setScreenAndShow(parent);
    }

    private static boolean inside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
}
