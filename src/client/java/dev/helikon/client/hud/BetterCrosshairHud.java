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
    private final HudLayout layout;

    public BetterCrosshairHud(BetterCrosshair module, PanicState panicState) {
        this(module, panicState, new HudLayout());
    }

    public BetterCrosshairHud(BetterCrosshair module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || !layout.element(HudElementId.BETTER_CROSSHAIR).enabled()
                || panicState.customHudHidden()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        int movementGap = movementGap(client);
        int diameter = crosshairDiameter(movementGap);
        HudElementPlacement placement = layout.element(HudElementId.BETTER_CROSSHAIR);
        HudPresentation.Frame frame = module.frameEnabled()
                ? HudPresentation.beginFrame(graphics, placement, diameter, diameter)
                : HudPresentation.beginTransparentFrame(graphics, placement, diameter, diameter);
        int centerX = frame.contentX() + diameter / 2;
        int centerY = frame.contentY() + diameter / 2;
        for (CrosshairGeometry.Rect arm : CrosshairGeometry.arms(
                centerX, centerY, module.sizePixels(), module.gapPixels(), module.thicknessPixels(), movementGap
        )) {
            if (module.outlineEnabled()) {
                graphics.outline(arm.x() - 1, arm.y() - 1, arm.width() + 2, arm.height() + 2, OUTLINE_COLOR);
            }
            int color = placement.rainbow() || placement.color() != HudElementPlacement.DEFAULT_COLOR
                    ? HudPresentation.color(placement) : module.color();
            graphics.fill(arm.x(), arm.y(), arm.x() + arm.width(), arm.y() + arm.height(), color);
        }
        HudPresentation.endFrame(graphics);
    }

    private int movementGap(Minecraft client) {
        if (!module.dynamicMovementEnabled() || client.player == null) {
            return 0;
        }
        return Math.clamp((int) Math.round(client.player.getDeltaMovement().horizontalDistance() * 20.0),
                0, MAX_MOVEMENT_GAP);
    }

    private int crosshairDiameter(int movementGap) {
        return 2 * (module.sizePixels() + module.gapPixels() + movementGap) + 3;
    }
}
