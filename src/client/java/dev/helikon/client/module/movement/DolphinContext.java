package dev.helikon.client.module.movement;

/** Minecraft-free local facts used to decide whether Dolphin may request normal jump input. */
public record DolphinContext(
        boolean screenOpen,
        boolean inWater,
        boolean movingForward,
        boolean moving,
        boolean sneaking,
        boolean passenger,
        boolean abilityFlying,
        boolean fallFlying
) {
}
