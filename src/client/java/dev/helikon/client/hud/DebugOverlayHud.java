package dev.helikon.client.hud;

import dev.helikon.client.config.ConfigurationManager;
import dev.helikon.client.event.EventBus;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.ModuleTimingMetrics;
import dev.helikon.client.module.miscellaneous.DebugOverlay;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.MinecraftWorldVisualizationRenderer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Opt-in local performance diagnostics; it never submits telemetry or writes a file. */
public final class DebugOverlayHud implements HudElement {
    private static final int PADDING = 3;

    private final DebugOverlay module;
    private final ModuleRegistry modules;
    private final ModuleTimingMetrics timings;
    private final MinecraftWorldVisualizationRenderer worldVisuals;
    private final EventBus events;
    private final ConfigurationManager configuration;
    private final PanicState panicState;
    private final HudLayout layout;
    private RenderState renderState = RenderState.empty();

    public DebugOverlayHud(DebugOverlay module, ModuleRegistry modules, ModuleTimingMetrics timings,
                           MinecraftWorldVisualizationRenderer worldVisuals, EventBus events,
                           ConfigurationManager configuration, PanicState panicState) {
        this(module, modules, timings, worldVisuals, events, configuration, panicState, new HudLayout());
    }

    public DebugOverlayHud(DebugOverlay module, ModuleRegistry modules, ModuleTimingMetrics timings,
                           MinecraftWorldVisualizationRenderer worldVisuals, EventBus events,
                           ConfigurationManager configuration, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.modules = Objects.requireNonNull(modules, "modules");
        this.timings = Objects.requireNonNull(timings, "timings");
        this.worldVisuals = Objects.requireNonNull(worldVisuals, "worldVisuals");
        this.events = Objects.requireNonNull(events, "events");
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || !layout.element(HudElementId.DEBUG_OVERLAY).enabled()
                || panicState.customHudHidden()) {
            return;
        }

        Minecraft client = Minecraft.getInstance();
        RenderState state = renderState;
        if (state.lines().isEmpty()) {
            return;
        }
        HudBounds bounds = layout.element(HudElementId.DEBUG_OVERLAY)
                .bounds(graphics.guiWidth(), graphics.guiHeight(), state.width(), state.height());
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + state.width(), bounds.y() + state.height(), 0xC014161B);
        for (int index = 0; index < state.lines().size(); index++) {
            graphics.text(client.font, state.lines().get(index), bounds.x() + PADDING,
                    bounds.y() + PADDING + index * client.font.lineHeight, 0xFFE5EDF5, true);
        }
    }

    /** Rebuilds the local text only once per client tick, never from the per-frame HUD extraction path. */
    public void tick() {
        if (!module.isEnabled() || panicState.customHudHidden()) {
            renderState = RenderState.empty();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        List<Component> lines = new ArrayList<>();
        int maximumWidth = 0;
        for (String line : buildLines()) {
            Component component = Component.literal(line);
            lines.add(component);
            maximumWidth = Math.max(maximumWidth, client.font.width(component));
        }
        renderState = new RenderState(List.copyOf(lines), maximumWidth + PADDING * 2,
                lines.size() * client.font.lineHeight + PADDING * 2);
    }

    private List<String> buildLines() {
        List<ModuleTimingMetrics.Snapshot> snapshots = timings.snapshots(modules.all());
        return DebugOverlayLines.format(snapshots, module.page(), worldVisuals.blockEspCacheSize(),
                worldVisuals.storageEspCacheSize(), events.subscriberCount(), configuration.saveStatus().displayName());
    }

    /** Prebuilt client-thread data consumed by the per-frame HUD extractor without collection work. */
    private record RenderState(List<Component> lines, int width, int height) {
        private RenderState {
            lines = List.copyOf(Objects.requireNonNull(lines, "lines"));
            if (width < 0 || height < 0) {
                throw new IllegalArgumentException("Debug overlay dimensions must not be negative");
            }
        }

        private static RenderState empty() {
            return new RenderState(List.of(), 0, 0);
        }
    }
}
