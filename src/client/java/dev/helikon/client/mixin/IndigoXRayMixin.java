package dev.helikon.client.mixin;

import dev.helikon.client.module.render.XRayRenderAccess;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.MutableQuadView;
import net.fabricmc.fabric.api.client.renderer.v1.mesh.ShadeMode;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors the vanilla XRay renderer hooks in Fabric's default Indigo chunk renderer.
 * Fabric replaces {@code ModelBlockRenderer} during section compilation in 26.2, so
 * the vanilla-only hook never sees normal chunk-model quads.
 */
@Mixin(targets = "net.fabricmc.fabric.impl.client.indigo.renderer.render.AltModelBlockRendererImpl")
abstract class IndigoXRayMixin {
    private static final int FULL_BRIGHT_LIGHTMAP = 0x00F000F0;

    @Redirect(method = "shouldCullFace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private boolean helikon$keepSelectedBlockFaces(BlockState state, BlockState adjacentState, Direction direction) {
        return XRayRenderAccess.isCompilingXRayChunk() || Block.shouldRenderFace(state, adjacentState, direction);
    }

    @Inject(method = "transform", at = @At("HEAD"))
    private void helikon$prepareXRayLighting(MutableQuadView quad, CallbackInfoReturnable<Boolean> callback) {
        if (XRayRenderAccess.isCompilingXRayChunk()) {
            quad.emissive(true);
            quad.ambientOcclusion(TriState.FALSE);
            quad.shadeMode(ShadeMode.ENHANCED);
        }
    }

    @Inject(method = "transform", at = @At("TAIL"))
    private void helikon$applyXRayOpacity(MutableQuadView quad, CallbackInfoReturnable<Boolean> callback) {
        if (!XRayRenderAccess.isCompilingXRayChunk()) {
            return;
        }
        float opacity = XRayRenderAccess.opacity();
        for (int index = 0; index < 4; index++) {
            int color = quad.color(index);
            int alpha = Math.round(((color >>> 24) & 0xFF) * opacity);
            quad.color(index, (color & 0x00FFFFFF) | (alpha << 24));
            quad.lightmap(index, FULL_BRIGHT_LIGHTMAP);
        }
        quad.chunkLayer(ChunkSectionLayer.TRANSLUCENT);
    }
}
