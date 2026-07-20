package dev.helikon.client.hud;

import dev.helikon.client.module.render.Coordinates;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/** Draws the local player's current position only while the Coordinates module is enabled. */
public final class LiveCoordinatesHud implements HudElement {
    private final Coordinates module;
    private final PanicState panicState;
    private final HudLayout layout;

    public LiveCoordinatesHud(Coordinates module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        HudElementPlacement placement = layout.element(HudElementId.LIVE_COORDINATES);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden()
                || client.player == null || client.level == null) {
            return;
        }
        List<String> lines = CoordinateReadout.lines(
                client.player.getX(), client.player.getY(), client.player.getZ(),
                client.level.dimension().identifier().toString(), module.decimals(), module.showDimension());
        HudPresentation.drawLines(graphics, client.font, lines, placement);
    }
}
