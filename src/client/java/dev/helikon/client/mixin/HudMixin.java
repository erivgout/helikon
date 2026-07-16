package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.equipment.Equippable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** Keeps AntiBlind and BetterCrosshair HUD interception narrow and local-only. */
@Mixin(targets = "net.minecraft.client.gui.Hud")
abstract class HudMixin {
    @Inject(method = "extractCrosshair", at = @At("HEAD"), cancellable = true)
    private void helikon$hideVanillaCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker,
                                               CallbackInfo callback) {
        if (RenderModuleAccess.hideVanillaCrosshair()) {
            callback.cancel();
        }
    }

    @Redirect(
            method = "extractCameraOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F")
    )
    private float helikon$hideNausea(LocalPlayer player, Holder<MobEffect> effect, float tickDelta) {
        if (effect == MobEffects.NAUSEA && RenderModuleAccess.hideNausea()) {
            return 0.0F;
        }
        return player.getEffectBlendFactor(effect, tickDelta);
    }

    @Redirect(
            method = "extractCameraOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getTicksFrozen()I")
    )
    private int helikon$hidePowderSnowOverlay(LocalPlayer player) {
        return RenderModuleAccess.hidePowderSnowOverlay() ? 0 : player.getTicksFrozen();
    }

    @Redirect(
            method = "extractCameraOverlays",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/equipment/Equippable;cameraOverlay()Ljava/util/Optional;")
    )
    private Optional<Identifier> helikon$hidePumpkinOverlay(Equippable equippable) {
        Optional<Identifier> overlay = equippable.cameraOverlay();
        if (!RenderModuleAccess.hidePumpkinOverlay()) {
            return overlay;
        }
        return overlay.filter(identifier -> !identifier.getPath().contains("pumpkin"));
    }
}
