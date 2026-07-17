package dev.helikon.client.hud;

import dev.helikon.client.module.render.Health;
import dev.helikon.client.module.render.HealthReadout;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;

import java.util.List;
import java.util.Objects;

/** Thin HUD renderer that draws the local player's health near the crosshair. */
public final class HealthHud implements HudElement {
    private final Health module;
    private final PanicState panicState;
    private final HudLayout layout;

    public HealthHud(Health module, PanicState panicState) {
        this(module, panicState, new HudLayout());
    }

    public HealthHud(Health module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        HudElementPlacement placement = layout.element(HudElementId.HEALTH);
        LocalPlayer player = client.player;
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden() || player == null) {
            return;
        }
        float health = player.getHealth();
        float maxHealth = player.getMaxHealth();
        float absorption = player.getAbsorptionAmount();
        String text = HealthReadout.text(health, maxHealth, absorption,
                module.showMax(), module.showAbsorption(), module.showDecimals());
        if (module.colorByHealth()) {
            HudPresentation.drawLines(graphics, client.font, List.of(text), placement,
                    HealthReadout.color(health, maxHealth));
        } else {
            HudPresentation.drawLines(graphics, client.font, List.of(text), placement);
        }
    }
}
