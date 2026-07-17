package dev.helikon.client.module.movement;

import net.minecraft.client.player.LocalPlayer;

import java.util.Objects;

/** Narrow Minecraft-facing bridge for the verified LivingEntity pushability gate. */
public final class AntiEntityPushAccess {
    private static volatile AntiEntityPush module;

    private AntiEntityPushAccess() {
    }

    public static void install(AntiEntityPush antiEntityPush) {
        module = Objects.requireNonNull(antiEntityPush, "antiEntityPush");
    }

    public static boolean preventCollisionPush(LocalPlayer player) {
        AntiEntityPush current = module;
        return current != null && current.preventsCollisionPush(player.isShiftKeyDown());
    }
}
