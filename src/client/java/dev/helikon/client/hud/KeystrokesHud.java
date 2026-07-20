package dev.helikon.client.hud;

import dev.helikon.client.module.render.Keystrokes;
import dev.helikon.client.module.render.RainbowUiAccess;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

/** Draws compact movement, mouse, and jump key state using the player's configured controls. */
public final class KeystrokesHud implements HudElement {
    private static final int KEY = 18;
    private static final int GAP = 2;
    private static final int MOVEMENT_WIDTH = KEY * 3 + GAP * 2;
    private static final int WIDTH = 68;
    private static final int INACTIVE = 0xB022252C;
    private static final int ACTIVE_TEXT = 0xFF14161B;

    private final Keystrokes module;
    private final PanicState panicState;
    private final HudLayout layout;

    public KeystrokesHud(Keystrokes module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        HudElementPlacement placement = layout.element(HudElementId.KEYSTROKES);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden()) {
            return;
        }

        int row = KEY + GAP;
        int height = KEY * 2 + GAP;
        int mouseHeight = module.showCps() ? 28 : KEY;
        if (module.showMouseButtons()) {
            height += mouseHeight + GAP;
        }
        if (module.showJump()) {
            height += KEY + GAP;
        }
        HudPresentation.Frame frame = HudPresentation.beginTransparentFrame(graphics, placement, WIDTH, height);
        int x = frame.contentX();
        int y = frame.contentY();
        int movementX = x + (WIDTH - MOVEMENT_WIDTH) / 2;
        long nowMillis = System.currentTimeMillis();
        int accent = RainbowUiAccess.accent(nowMillis, HudPresentation.color(placement));
        drawKey(graphics, client.font, keyName(client.options.keyUp.getTranslatedKeyMessage().getString()),
                movementX + row, y, KEY, KEY, client.options.keyUp.isDown(), placement, accent);
        drawKey(graphics, client.font, keyName(client.options.keyLeft.getTranslatedKeyMessage().getString()),
                movementX, y + row, KEY, KEY, client.options.keyLeft.isDown(), placement, accent);
        drawKey(graphics, client.font, keyName(client.options.keyDown.getTranslatedKeyMessage().getString()),
                movementX + row, y + row, KEY, KEY, client.options.keyDown.isDown(), placement, accent);
        drawKey(graphics, client.font, keyName(client.options.keyRight.getTranslatedKeyMessage().getString()),
                movementX + row * 2, y + row, KEY, KEY, client.options.keyRight.isDown(), placement, accent);

        int nextY = y + row * 2;
        if (module.showMouseButtons()) {
            int mouseWidth = (WIDTH - GAP) / 2;
            if (module.showCps()) {
                long nowNanos = System.nanoTime();
                drawMouseKey(graphics, client.font, "LMB",
                        module.clicksPerSecond(ClickRateTracker.Button.LEFT, nowNanos),
                        x, nextY, mouseWidth, mouseHeight, client.options.keyAttack.isDown(), placement, accent);
                drawMouseKey(graphics, client.font, "RMB",
                        module.clicksPerSecond(ClickRateTracker.Button.RIGHT, nowNanos),
                        x + mouseWidth + GAP, nextY, mouseWidth, mouseHeight,
                        client.options.keyUse.isDown(), placement, accent);
            } else {
                drawKey(graphics, client.font, "LMB", x, nextY, mouseWidth, mouseHeight,
                        client.options.keyAttack.isDown(), placement, accent);
                drawKey(graphics, client.font, "RMB", x + mouseWidth + GAP, nextY, mouseWidth, mouseHeight,
                        client.options.keyUse.isDown(), placement, accent);
            }
            nextY += mouseHeight + GAP;
        }
        if (module.showJump()) {
            drawKey(graphics, client.font, "SPACE", x, nextY, WIDTH, KEY,
                    client.options.keyJump.isDown(), placement, accent);
        }
        HudPresentation.endFrame(graphics);
    }

    private static void drawKey(GuiGraphicsExtractor graphics, Font font, String label, int x, int y,
                                int width, int height, boolean pressed, HudElementPlacement placement, int accent) {
        graphics.fill(x, y, x + width, y + height, pressed ? accent : INACTIVE);
        graphics.outline(x, y, width, height, accent);
        int color = pressed ? ACTIVE_TEXT : accent;
        graphics.text(font, label, x + (width - font.width(label)) / 2,
                y + (height - font.lineHeight) / 2 + 1, color, placement.textShadow() && !pressed);
    }

    private static void drawMouseKey(GuiGraphicsExtractor graphics, Font font, String label, int cps, int x, int y,
                                     int width, int height, boolean pressed, HudElementPlacement placement, int accent) {
        graphics.fill(x, y, x + width, y + height, pressed ? accent : INACTIVE);
        graphics.outline(x, y, width, height, accent);
        int color = pressed ? ACTIVE_TEXT : accent;
        boolean shadow = placement.textShadow() && !pressed;
        graphics.text(font, label, x + (width - font.width(label)) / 2, y + 3, color, shadow);
        String rate = Math.min(99, Math.max(0, cps)) + " CPS";
        graphics.text(font, rate, x + (width - font.width(rate)) / 2, y + 3 + font.lineHeight, color, shadow);
    }

    static String keyName(String translatedName) {
        if (translatedName == null || translatedName.isBlank()) {
            return "?";
        }
        String name = translatedName.trim().toUpperCase(java.util.Locale.ROOT);
        if (name.startsWith("LEFT ")) {
            name = name.substring(5);
        } else if (name.startsWith("RIGHT ")) {
            name = name.substring(6);
        }
        if (name.equals("CONTROL") || name.equals("CTRL")) {
            return "CTL";
        }
        if (name.equals("SPACE")) {
            return "SPC";
        }
        return name.length() <= 3 ? name : name.substring(0, 3);
    }
}
