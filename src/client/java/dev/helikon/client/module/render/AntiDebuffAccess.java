package dev.helikon.client.module.render;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

import java.util.Collection;
import java.util.Objects;

/** Narrow bridge from the verified effect-particle render hook to AntiDebuff's Minecraft-free policy. */
public final class AntiDebuffAccess {
    private static volatile AntiDebuff module;

    private AntiDebuffAccess() {
    }

    public static void install(AntiDebuff antiDebuff) {
        module = Objects.requireNonNull(antiDebuff, "antiDebuff");
    }

    /**
     * Reports whether the local player's swirling effect particles should be
     * suppressed. Only the local player is considered; other entities keep
     * their vanilla particles.
     */
    public static boolean hideLocalEffectParticles(LivingEntity entity) {
        AntiDebuff current = module;
        if (current == null || !(entity instanceof LocalPlayer)) {
            return false;
        }
        Collection<MobEffectInstance> effects = entity.getActiveEffects();
        boolean anyEffectActive = !effects.isEmpty();
        boolean allHarmful = anyEffectActive && effects.stream()
                .allMatch(effect -> effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL);
        return current.hidesEffectParticles(anyEffectActive, allHarmful);
    }
}
