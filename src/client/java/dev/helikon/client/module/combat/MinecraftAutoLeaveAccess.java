package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/** Small 26.2 adapter that reads local player facts and uses Minecraft's ordinary disconnect flow. */
public final class MinecraftAutoLeaveAccess {
    private MinecraftAutoLeaveAccess() {
    }

    public static Optional<AutoLeave.Danger> observedDanger(AutoLeave module) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.getConnection() == null
                || client.getCurrentServer() == null || client.isLocalServer()) {
            return Optional.empty();
        }
        return module.danger(new AutoLeave.Context(
                client.player.getHealth() + client.player.getAbsorptionAmount(), client.player.fallDistance));
    }

    public static void disconnect(AutoLeave.Danger danger) {
        Minecraft.getInstance().disconnectFromWorld(Component.literal("AutoLeave: " + describe(danger)));
    }

    private static String describe(AutoLeave.Danger danger) {
        return switch (danger) {
            case LOW_HEALTH -> "low health";
            case FALL_DISTANCE -> "dangerous fall";
        };
    }
}
