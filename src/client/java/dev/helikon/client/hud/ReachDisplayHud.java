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
        int width = client.font.width(text) + 6;
        int height = client.font.lineHeight + 6;
        HudBounds bounds = placement.bounds(graphics.guiWidth(), graphics.guiHeight(), width, height);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + width, bounds.y() + height, 0xB014161B);
        graphics.text(client.font, Component.literal(text), bounds.x() + 3, bounds.y() + 3, 0xFFE5EDF5, true);
    }
}
