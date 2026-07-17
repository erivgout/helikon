package dev.helikon.client.hud;

import dev.helikon.client.module.render.SaturationDisplay;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;
import java.util.Objects;

/** Small local HUD readout for Minecraft's currently observed hunger saturation. */
public final class SaturationHud implements HudElement {
    private static final int X = 5;

    private final SaturationDisplay module;
    private final PanicState panicState;
    private final HudLayout layout;

    public SaturationHud(SaturationDisplay module, PanicState panicState) {
        this(module, panicState, new HudLayout());
    }

    public SaturationHud(SaturationDisplay module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        HudElementPlacement placement = layout.element(HudElementId.SATURATION);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden() || client.player == null) {
            return;
        }
        String text = format(client.player.getFoodData().getSaturationLevel());
        HudPresentation.drawLines(graphics, client.font, java.util.List.of(text), placement);
    }

    static String format(float saturation) {
        if (!Float.isFinite(saturation) || saturation < 0.0F) {
            return "Saturation --";
        }
        return String.format(Locale.ROOT, "Saturation %.1f", saturation);
    }
}
