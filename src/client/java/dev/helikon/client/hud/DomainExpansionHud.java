package dev.helikon.client.hud;

import dev.helikon.client.gui.ClickGuiTheme;
import dev.helikon.client.gui.ClickGuiWindowState;
import dev.helikon.client.module.combat.DomainExpansion;
import dev.helikon.client.module.render.RainbowUiAccess;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.Locale;
import java.util.Objects;

/** Compact top-center status HUD for the active Domain Expansion construction. */
public final class DomainExpansionHud implements HudElement {
    private final DomainExpansion module;
    private final PanicState panic;
    private final ClickGuiWindowState window;

    public DomainExpansionHud(DomainExpansion module, PanicState panic, ClickGuiWindowState window) {
        this.module = Objects.requireNonNull(module, "module");
        this.panic = Objects.requireNonNull(panic, "panic");
        this.window = Objects.requireNonNull(window, "window");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        DomainExpansion.RenderSnapshot snapshot = module.renderSnapshot();
        if (!module.isEnabled() || !module.showCompletion() || panic.customHudHidden()
                || snapshot.targetName().isBlank()
                || snapshot.state() == DomainExpansion.State.IDLE
                || snapshot.state() == DomainExpansion.State.ARMED) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        ClickGuiTheme theme = window.theme();
        int accent = RainbowUiAccess.accent(System.currentTimeMillis(), theme.accent());
        String title = snapshot.state() == DomainExpansion.State.COMPLETE
                ? "DOMAIN COMPLETE" : "DOMAIN EXPANSION";
        String detail = String.format(Locale.ROOT, "%s  %d%%  %s", snapshot.targetName(),
                Math.round(snapshot.completion() * 100.0D),
                snapshot.state().name().toLowerCase(Locale.ROOT).replace('_', ' '));
        int width = Math.max(client.font.width(title), client.font.width(detail)) + 16;
        int x = (graphics.guiWidth() - width) / 2;
        graphics.fill(x, 6, x + width, 34, theme.panel());
        graphics.outline(x, 6, width, 28, accent);
        graphics.centeredText(client.font, title, graphics.guiWidth() / 2, 10, accent);
        graphics.centeredText(client.font, detail, graphics.guiWidth() / 2, 22, theme.text());
    }
}
