package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.client.renderer.state.level.WeatherRenderState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Clears the freshly extracted local precipitation columns when NoWeather requests it, so the
 * following vanilla weather render draws nothing. It never alters weather packets, world state, or
 * lighting; disabling NoWeather lets the next extraction repopulate columns and restore vanilla
 * rendering.
 */
@Mixin(WeatherEffectRenderer.class)
abstract class WeatherEffectRendererMixin {
    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void helikon$hideWeather(ClientLevel level, float partialTick, Vec3 cameraPosition,
                                     WeatherRenderState renderState, CallbackInfo callback) {
        if (RenderModuleAccess.hideRainColumns()) {
            renderState.rainColumns.clear();
        }
        if (RenderModuleAccess.hideSnowColumns()) {
            renderState.snowColumns.clear();
        }
    }
}
