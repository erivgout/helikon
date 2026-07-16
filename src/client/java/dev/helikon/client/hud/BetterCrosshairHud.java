package dev.helikon.client.hud;

import dev.helikon.client.module.render.BetterCrosshair;
import dev.helikon.client.module.render.CrosshairGeometry;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Objects;

/** Thin HUD renderer for BetterCrosshair's Minecraft-free arm geometry. */
public final class BetterCrosshairHud implements HudElement {
    private static final int OUTLINE_COLOR = 0xD0000000;
    private static final int MAX_MOVEMENT_GAP = 4;

    private final BetterCrosshair module;
    private final PanicState panicState;

    public BetterCrosshairHud(BetterCrosshair module, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || panicState.customHudHidden()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int movementGap = movementGap(client);
        int centerX = client.getWindow().getGuiScaledWidth() / 2;
        int centerY = client.getWindow().getGuiScaledHeight() / 2;
        for (CrosshairGeometry.Rect arm : CrosshairGeometry.arms(
                centerX, centerY, module.sizePixels(), module.gapPixels(), module.thicknessPixels(), movementGap
        )) {
            if (module.outlineEnabled()) {
                graphics.outline(arm.x() - 1, arm.y() - 1, arm.width() + 2, arm.height() + 2, OUTLINE_COLOR);
            }
            graphics.fill(arm.x(), arm.y(), arm.x() + arm.width(), arm.y() + arm.height(), module.color());
        }
    }

    private int movementGap(Minecraft client) {
        if (!module.dynamicMovementEnabled() || client.player == null) {
            return 0;
        }
        return Math.clamp((int) Math.round(client.player.getDeltaMovement().horizontalDistance() * 20.0),
                0, MAX_MOVEMENT_GAP);
    }
}
