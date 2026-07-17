package dev.helikon.client.hud;

import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.module.combat.ReachDisplay;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;
import java.util.Objects;

/** Displays only the distance measured for Helikon's last ordinary local attack request. */
public final class ReachDisplayHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 324;

    private final ReachDisplay module;
    private final CombatTargetTracker tracker;
    private final PanicState panicState;
    private final HudLayout layout;

    public ReachDisplayHud(ReachDisplay module, CombatTargetTracker tracker, PanicState panicState) {
        this(module, tracker, panicState, new HudLayout());
    }

    public ReachDisplayHud(ReachDisplay module, CombatTargetTracker tracker, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        HudElementPlacement placement = layout.element(HudElementId.REACH);
        if (!module.isEnabled() || !placement.enabled() || panicState.customHudHidden()) {
            return;
        }
        String text = tracker.lastAttackDistance().map(distance -> String.format(Locale.ROOT, "Reach %.2f", distance))
                .orElse("Reach --");
        Minecraft client = Minecraft.getInstance();
        HudPresentation.drawLines(graphics, client.font, java.util.List.of(text), placement);
    }
}
