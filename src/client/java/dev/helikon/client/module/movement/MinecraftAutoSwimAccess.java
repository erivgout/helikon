package dev.helikon.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;

/** Narrow 26.2 bridge for AutoSwim's normal local sprint-state requests. */
public final class MinecraftAutoSwimAccess {
    private MinecraftAutoSwimAccess() {
    }

    public static void tick(AutoSwim autoSwim) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            autoSwim.onPlayerUnavailable();
            return;
        }
        LocalPlayer player = client.player;
        Input input = player.input.keyPresses;
        AutoSwim.SprintAction action = autoSwim.update(new AutoSwim.Context(
                dev.helikon.client.gui.GameplayScreenPolicy.blocksAutomation(client.gui.screen()),
                player.isInWater(),
                input.forward(),
                player.isPassenger(),
                player.getAbilities().flying,
                player.getFoodData().getFoodLevel(),
                player.isSprinting()
        ));
        switch (action) {
            case START -> player.setSprinting(true);
            case STOP -> player.setSprinting(false);
            case NONE -> {
                // No normal sprint-state transition is needed this tick.
            }
        }
    }
}
