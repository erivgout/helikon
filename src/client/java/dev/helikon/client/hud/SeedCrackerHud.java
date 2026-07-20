package dev.helikon.client.hud;

import dev.helikon.client.module.world.SeedCracker;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.List;
import java.util.Objects;

/** Compact themed progress HUD for SeedCracker collection and candidate filtering. */
public final class SeedCrackerHud implements HudElement {
    private final SeedCracker module;
    private final PanicState panicState;
    private final HudLayout layout;

    public SeedCrackerHud(SeedCracker module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        HudElementPlacement placement = layout.element(HudElementId.SEED_CRACKER);
        if (!module.isEnabled() || !module.showHud() || !placement.enabled()
                || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        List<String> lines = module.statusLines();
        HudPresentation.drawLines(graphics, client.font, lines, placement);
    }
}
