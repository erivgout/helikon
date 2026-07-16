package dev.helikon.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Applies an invisible, client-local Night Vision instance and restores the
 * effect that was present before Helikon took ownership of the visual state.
 */
public final class MinecraftNightVisionAccess implements Fullbright.NightVisionAccess {
    private final ClientEffectOverrideState<MobEffectInstance> effectOverride = new ClientEffectOverrideState<>();

    private LocalPlayer player;

    @Override
    public void apply() {
        LocalPlayer currentPlayer = Minecraft.getInstance().player;
        if (currentPlayer == null) {
            return;
        }

        if (currentPlayer != player) {
            restore();
            player = currentPlayer;
        }

        MobEffectInstance currentEffect = player.getEffect(MobEffects.NIGHT_VISION);
        MobEffectInstance helikonEffect = effectOverride.apply(currentEffect, MinecraftNightVisionAccess::createHelikonEffect);
        if (helikonEffect != currentEffect) {
            player.forceAddEffect(helikonEffect, null);
        }
    }

    @Override
    public void restore() {
        if (player == null) {
            effectOverride.restore(null);
            clear();
            return;
        }

        ClientEffectOverrideState.Restoration<MobEffectInstance> restoration =
                effectOverride.restore(player.getEffect(MobEffects.NIGHT_VISION));
        if (restoration.removeOverride()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
            if (restoration.original() != null) {
                player.forceAddEffect(new MobEffectInstance(restoration.original()), null);
            }
        }
        clear();
    }

    private static MobEffectInstance createHelikonEffect() {
        return new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                MobEffectInstance.INFINITE_DURATION,
                0,
                true,
                false,
                false
        );
    }

    private void clear() {
        player = null;
    }
}
