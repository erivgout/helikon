package dev.helikon.client.mixin;

import dev.helikon.client.module.render.AntiDebuffAccess;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets AntiDebuff drop only the local player's client-side effect-particle
 * spawn in {@code tickEffects}. The redirect leaves server tick logic, effect
 * durations, and every other entity untouched.
 */
@Mixin(LivingEntity.class)
abstract class LivingEntityEffectParticleMixin {
    @Redirect(
            method = "tickEffects",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V")
    )
    private void helikon$suppressLocalEffectParticles(Level level, ParticleOptions particle,
                                                      double x, double y, double z,
                                                      double xSpeed, double ySpeed, double zSpeed) {
        if (AntiDebuffAccess.hideLocalEffectParticles((LivingEntity) (Object) this)) {
            return;
        }
        level.addParticle(particle, x, y, z, xSpeed, ySpeed, zSpeed);
    }
}
