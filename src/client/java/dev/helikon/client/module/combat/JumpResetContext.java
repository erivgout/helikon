package dev.helikon.client.module.combat;

/** Minecraft-free local facts for one ordinary jump-reset request after receiving a hit. */
public record JumpResetContext(
        boolean screenOpen,
        boolean onGround,
        boolean freshHit,
        boolean movingHorizontally
) {
}
