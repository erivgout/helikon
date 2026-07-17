package dev.helikon.client.hud;

import dev.helikon.client.module.render.SaturationDisplay;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

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
        int width = client.font.width(text) + 6;
        int height = client.font.lineHeight + 6;
        HudBounds bounds = placement.bounds(graphics.guiWidth(), graphics.guiHeight(), width, height);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + width, bounds.y() + height, 0xB014161B);
        graphics.text(client.font, Component.literal(text), bounds.x() + 3, bounds.y() + 3, 0xFFE5EDF5, true);
    }

    static String format(float saturation) {
        if (!Float.isFinite(saturation) || saturation < 0.0F) {
            return "Saturation --";
        }
        return String.format(Locale.ROOT, "Saturation %.1f", saturation);
    }
}
