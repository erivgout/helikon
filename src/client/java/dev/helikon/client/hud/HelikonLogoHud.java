package dev.helikon.client.hud;

import dev.helikon.client.module.render.HelikonLogo;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Fixed, compact original wordmark renderer. */
public final class HelikonLogoHud implements HudElement {
    private final HelikonLogo module;
    private final PanicState panic;

    public HelikonLogoHud(HelikonLogo module, PanicState panic) {
        this.module = module;
        this.panic = panic;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || panic.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        graphics.text(client.font, "HELIKON", 6, 6, module.color(), true);
    }
}
