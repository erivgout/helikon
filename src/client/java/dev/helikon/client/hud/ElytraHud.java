package dev.helikon.client.hud;

import dev.helikon.client.module.movement.ExtraElytra;
import dev.helikon.client.module.movement.MinecraftAdvancedMovementAccess;
import dev.helikon.client.panic.PanicState;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;

/** Small local ExtraElytra HUD readout for observed speed and durability warning state. */
public final class ElytraHud implements HudElement {
    private static final int X = 5;
    private static final int Y = 222;

    private final ExtraElytra module;
    private final PanicState panicState;

    public ElytraHud(ExtraElytra module, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (!module.showSpeed() || panicState.customHudHidden() || client.player == null || !client.player.isFallFlying()) {
            return;
        }
        ExtraElytra.Status status = MinecraftAdvancedMovementAccess.elytraStatus(module, client.player);
        String speed = String.format(Locale.ROOT, "Elytra %.2f b/t", status.speed());
        int width = Math.max(client.font.width(speed), status.lowDurability() ? client.font.width("Elytra durability low") : 0) + 6;
        int height = client.font.lineHeight + (status.lowDurability() ? client.font.lineHeight : 0) + 6;
        graphics.fill(X, Y, X + width, Y + height, 0xB014161B);
        graphics.text(client.font, Component.literal(speed), X + 3, Y + 3, 0xFFE5EDF5, true);
        if (status.lowDurability()) {
            graphics.text(client.font, Component.literal("Elytra durability low"), X + 3, Y + 3 + client.font.lineHeight,
                    0xFFFF8A80, true);
        }
    }
}
