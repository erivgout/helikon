package dev.helikon.client.hud;

import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.module.combat.ReachDisplay;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;

/** Displays only the distance measured for Helikon's last ordinary local attack request. */
public final class ReachDisplayHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 324;

    private final ReachDisplay module;
    private final CombatTargetTracker tracker;
    private final PanicState panicState;

    public ReachDisplayHud(ReachDisplay module, CombatTargetTracker tracker, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || panicState.customHudHidden()) {
            return;
        }
        String text = tracker.lastAttackDistance().map(distance -> String.format(Locale.ROOT, "Reach %.2f", distance))
                .orElse("Reach --");
        Minecraft client = Minecraft.getInstance();
        int width = client.font.width(text) + 6;
        graphics.fill(X, Y, X + width, Y + client.font.lineHeight + 6, 0xB014161B);
        graphics.text(client.font, Component.literal(text), X + 3, Y + 3, 0xFFE5EDF5, true);
    }
}
