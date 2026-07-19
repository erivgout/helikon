package dev.helikon.client.module.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;

import java.util.Objects;

/** Thin Minecraft adapter for the limited miscellaneous actions that need a client tick. */
public final class MinecraftMiscellaneousAccess {
    private MinecraftMiscellaneousAccess() {
    }

    /** Uses Minecraft's ordinary main-hand swing path only after the pure rate-limit policy permits it. */
    public static void tickAnnoy(Minecraft client, Annoy module, long clientTick) {
        Minecraft current = Objects.requireNonNull(client, "client");
        Annoy currentModule = Objects.requireNonNull(module, "module");
        if (currentModule.shouldSwing(clientTick,
                new Annoy.Context(current.player != null,
                        dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(current.gui.screen())))) {
            current.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
