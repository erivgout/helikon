package dev.helikon.client.hud;

import dev.helikon.client.module.render.Time;
import dev.helikon.client.module.render.RainbowUiAccess;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;

/** Draws the selected local or Minecraft-world clock while the Time module is enabled. */
public final class TimeHud implements HudElement {
    private final Time module;
    private final PanicState panicState;
    private final HudLayout layout;

    public TimeHud(Time module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        HudElementPlacement placement = layout.element(HudElementId.TIME);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden()) {
            return;
        }
        String line;
        if (module.source() == Time.Source.WORLD) {
            if (client.level == null) {
                return;
            }
            line = TimeReadout.world(client.level.getOverworldClockTime(), module.twentyFourHour());
        } else {
            line = TimeReadout.local(LocalTime.now(), module.twentyFourHour(), module.showSeconds());
        }
        int color = RainbowUiAccess.accent(System.currentTimeMillis(), HudPresentation.color(placement));
        HudPresentation.drawLines(graphics, client.font, List.of(line), placement, color);
    }
}
