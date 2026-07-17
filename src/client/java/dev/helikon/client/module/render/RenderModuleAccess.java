package dev.helikon.client.module.render;

import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.hud.HudElementId;
import dev.helikon.client.hud.HudLayout;
import dev.helikon.client.panic.PanicState;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;

import java.util.Objects;

/** Narrow static bridge used only by verified client-render mixins. */
public final class RenderModuleAccess {
    private static volatile AntiBlind antiBlind;
    private static volatile CameraNoClip cameraNoClip;
    private static volatile NoFireOverlay noFireOverlay;
    private static volatile NoFog noFog;
    private static volatile NoHurtcam noHurtcam;
    private static volatile NoShieldOverlay noShieldOverlay;
    private static volatile BetterCrosshair betterCrosshair;
    private static volatile AntiTotemAnimation antiTotemAnimation;
    private static volatile Dinnerbone dinnerbone;
    private static volatile RainbowEnchant rainbowEnchant;
    private static volatile HudLayout hudLayout;
    private static volatile PanicState panicState;

    private RenderModuleAccess() {
    }

    public static void install(AntiBlind antiBlindModule, NoFireOverlay noFireOverlayModule, BetterCrosshair crosshairModule,
                               AntiTotemAnimation antiTotemAnimationModule, Dinnerbone dinnerboneModule,
                               RainbowEnchant rainbowEnchantModule, HudLayout layout, PanicState panic) {
        antiBlind = Objects.requireNonNull(antiBlindModule, "antiBlindModule");
        noFireOverlay = Objects.requireNonNull(noFireOverlayModule, "noFireOverlayModule");
        betterCrosshair = Objects.requireNonNull(crosshairModule, "crosshairModule");
        antiTotemAnimation = Objects.requireNonNull(antiTotemAnimationModule, "antiTotemAnimationModule");
        dinnerbone = Objects.requireNonNull(dinnerboneModule, "dinnerboneModule");
        rainbowEnchant = Objects.requireNonNull(rainbowEnchantModule, "rainbowEnchantModule");
        hudLayout = Objects.requireNonNull(layout, "layout");
        panicState = Objects.requireNonNull(panic, "panic");
    }

    public static boolean hideMobEffectFog(Holder<MobEffect> effect) {
        AntiBlind module = antiBlind;
        return module != null && ((effect == MobEffects.BLINDNESS && module.hidesBlindness())
                || (effect == MobEffects.DARKNESS && module.hidesDarkness()));
    }

    public static boolean hideNausea() {
        return antiBlind != null && antiBlind.hidesNausea();
    }

    public static void installCameraNoClip(CameraNoClip module) {
        cameraNoClip = Objects.requireNonNull(module, "module");
    }

    public static boolean useUnclippedCameraDistance() {
        CameraNoClip module = cameraNoClip;
        return module != null && module.usesUnclippedCameraDistance();
    }

    public static void installNoFog(NoFog module) {
        noFog = Objects.requireNonNull(module, "module");
    }

    public static NoFog.FogPlanes extendFog(NoFog.FogPlanes vanillaPlanes) {
        NoFog module = noFog;
        return module == null ? vanillaPlanes : module.extend(vanillaPlanes);
    }

    public static void installNoHurtcam(NoHurtcam module) {
        noHurtcam = Objects.requireNonNull(module, "module");
    }

    public static boolean hideHurtCamera() {
        NoHurtcam module = noHurtcam;
        return module != null && module.hidesHurtCamera();
    }

    public static boolean hidePumpkinOverlay() {
        return antiBlind != null && antiBlind.hidesPumpkinOverlay();
    }

    public static boolean hidePowderSnowOverlay() {
        return antiBlind != null && antiBlind.hidesPowderSnowOverlay();
    }

    public static boolean hideFireOverlay() {
        NoFireOverlay module = noFireOverlay;
        return module != null && module.hidesFireOverlay();
    }

    public static void installNoShieldOverlay(NoShieldOverlay module) {
        noShieldOverlay = Objects.requireNonNull(module, "module");
    }

    public static boolean hideRaisedShield(boolean usingItem, boolean usingShield) {
        NoShieldOverlay module = noShieldOverlay;
        return module != null && module.hidesRaisedShield(usingItem, usingShield);
    }

    public static boolean hideVanillaCrosshair() {
        BetterCrosshair module = betterCrosshair;
        HudLayout layout = hudLayout;
        PanicState panic = panicState;
        return module != null && layout != null && panic != null && module.hidesVanillaCrosshair()
                && layout.element(HudElementId.BETTER_CROSSHAIR).enabled() && !panic.customHudHidden();
    }

    public static boolean shouldHideItemActivation(boolean hasDeathProtection) {
        AntiTotemAnimation module = antiTotemAnimation;
        return module != null && module.shouldSuppressItemActivation(hasDeathProtection);
    }

    public static boolean shouldFlipDinnerbone(EntityRenderFilter.EntityType entityType) {
        Dinnerbone module = dinnerbone;
        return module != null && module.shouldFlip(entityType);
    }

    public static boolean rainbowEnchantEnabled() {
        RainbowEnchant module = rainbowEnchant;
        return module != null && module.isEnabled();
    }

    public static int rainbowEnchantColor(long nowMillis) {
        RainbowEnchant module = rainbowEnchant;
        return module == null ? 0xFFFFFFFF : module.glintColor(nowMillis);
    }
}
