package dev.helikon.client.module.render;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.Objects;

/** Narrow static bridge used only by verified client-render mixins. */
public final class RenderModuleAccess {
    private static volatile AntiBlind antiBlind;
    private static volatile BetterCrosshair betterCrosshair;

    private RenderModuleAccess() {
    }

    public static void install(AntiBlind antiBlindModule, BetterCrosshair crosshairModule) {
        antiBlind = Objects.requireNonNull(antiBlindModule, "antiBlindModule");
        betterCrosshair = Objects.requireNonNull(crosshairModule, "crosshairModule");
    }

    public static boolean hideMobEffectFog(Holder<MobEffect> effect) {
        AntiBlind module = antiBlind;
        return module != null && ((effect == MobEffects.BLINDNESS && module.hidesBlindness())
                || (effect == MobEffects.DARKNESS && module.hidesDarkness()));
    }

    public static boolean hideNausea() {
        return antiBlind != null && antiBlind.hidesNausea();
    }

    public static boolean hidePumpkinOverlay() {
        return antiBlind != null && antiBlind.hidesPumpkinOverlay();
    }

    public static boolean hidePowderSnowOverlay() {
        return antiBlind != null && antiBlind.hidesPowderSnowOverlay();
    }

    public static boolean hideVanillaCrosshair() {
        return betterCrosshair != null && betterCrosshair.hidesVanillaCrosshair();
    }
}
