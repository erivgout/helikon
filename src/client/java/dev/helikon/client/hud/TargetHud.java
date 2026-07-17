package dev.helikon.client.hud;

import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetTracker;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Local TargetHUD rendering only already-observed target facts; it does not query or alter game state. */
public final class TargetHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 260;

    private final dev.helikon.client.module.combat.TargetHud module;
    private final CombatTargetTracker tracker;
    private final PanicState panicState;
    private final HudLayout layout;

    public TargetHud(dev.helikon.client.module.combat.TargetHud module, CombatTargetTracker tracker, PanicState panicState) {
        this(module, tracker, panicState, new HudLayout());
    }

    public TargetHud(dev.helikon.client.module.combat.TargetHud module, CombatTargetTracker tracker, PanicState panicState,
                     HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.tracker = Objects.requireNonNull(tracker, "tracker");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || !layout.element(HudElementId.TARGET).enabled() || panicState.customHudHidden()) {
            return;
        }
        tracker.target().ifPresent(target -> render(graphics, Minecraft.getInstance(), target, layout.element(HudElementId.TARGET)));
    }

    private static void render(GuiGraphicsExtractor graphics, Minecraft client, CombatTarget target,
                               HudElementPlacement placement) {
        List<String> lines = List.of(
                target.name(),
                String.format(Locale.ROOT, "Health %.1f  Armor %d", target.health(), target.armor()),
                String.format(Locale.ROOT, "Distance %.2f", target.distance()),
                "Held " + target.heldItem(),
                target.effects().isEmpty() ? "Effects none" : "Effects " + String.join(", ", target.effects())
        );
        int width = lines.stream().mapToInt(client.font::width).max().orElse(0) + 8;
        int height = lines.size() * client.font.lineHeight + 8;
        HudBounds bounds = placement.bounds(graphics.guiWidth(), graphics.guiHeight(), width, height);
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + width, bounds.y() + height, 0xB014161B);
        for (int index = 0; index < lines.size(); index++) {
            graphics.text(client.font, Component.literal(lines.get(index)), bounds.x() + 4,
                    bounds.y() + 4 + index * client.font.lineHeight, index == 0 ? 0xFFFFD180 : 0xFFE5EDF5, true);
        }
    }
}
