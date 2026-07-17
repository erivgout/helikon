package dev.helikon.client.mixin;

import dev.helikon.client.module.render.NoFog;
import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends the completed local fog data only after Minecraft has selected its normal environment. */
@Mixin(targets = "net.minecraft.client.renderer.fog.FogRenderer")
abstract class NoFogMixin {
    @Inject(method = "setupFog", at = @At("RETURN"))
    private void helikon$extendFog(Camera camera, int renderDistance, DeltaTracker deltaTracker, float tickDelta,
                                   ClientLevel level, CallbackInfoReturnable<FogData> callback) {
        FogData data = callback.getReturnValue();
        NoFog.FogPlanes extended = RenderModuleAccess.extendFog(new NoFog.FogPlanes(
                data.environmentalStart, data.renderDistanceStart, data.environmentalEnd,
                data.renderDistanceEnd, data.skyEnd, data.cloudEnd
        ));
        data.environmentalStart = extended.environmentalStart();
        data.renderDistanceStart = extended.renderDistanceStart();
        data.environmentalEnd = extended.environmentalEnd();
        data.renderDistanceEnd = extended.renderDistanceEnd();
        data.skyEnd = extended.skyEnd();
        data.cloudEnd = extended.cloudEnd();
    }
}
