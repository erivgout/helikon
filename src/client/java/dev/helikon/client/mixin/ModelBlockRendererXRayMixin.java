package dev.helikon.client.mixin;

import com.mojang.blaze3d.vertex.QuadInstance;
import dev.helikon.client.module.render.XRayRenderAccess;
import net.minecraft.client.renderer.block.BlockQuadOutput;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Emits all selected-block faces and applies XRay opacity only during local chunk compilation. */
@Mixin(ModelBlockRenderer.class)
abstract class ModelBlockRendererXRayMixin {
    @Redirect(method = "shouldRenderFace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/Block;shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z"))
    private boolean helikon$keepSelectedBlockFaces(BlockState state, BlockState adjacentState, Direction direction) {
        return XRayRenderAccess.isCompilingXRayChunk() || Block.shouldRenderFace(state, adjacentState, direction);
    }

    @Redirect(method = "putQuadWithTint", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/block/BlockQuadOutput;put(FFFLnet/minecraft/client/resources/model/geometry/BakedQuad;Lcom/mojang/blaze3d/vertex/QuadInstance;)V"))
    private void helikon$applyXRayOpacity(BlockQuadOutput output, float x, float y, float z, BakedQuad quad,
                                          QuadInstance instance) {
        if (!XRayRenderAccess.isCompilingXRayChunk()) {
            output.put(x, y, z, quad, instance);
            return;
        }
        float opacity = XRayRenderAccess.opacity();
        for (int index = 0; index < BakedQuad.VERTEX_COUNT; index++) {
            int color = instance.getColor(index);
            int alpha = Math.round(((color >>> 24) & 0xFF) * opacity);
            instance.setColor(index, (color & 0x00FFFFFF) | (alpha << 24));
        }
        output.put(x, y, z, translucentQuad(quad), instance);
    }

    private static BakedQuad translucentQuad(BakedQuad quad) {
        BakedQuad.MaterialInfo material = quad.materialInfo();
        if (material.layer() == ChunkSectionLayer.TRANSLUCENT) {
            return quad;
        }
        BakedQuad.MaterialInfo translucentMaterial = new BakedQuad.MaterialInfo(material.sprite(),
                ChunkSectionLayer.TRANSLUCENT, material.itemRenderType(), material.tintIndex(), material.shade(),
                material.lightEmission());
        return new BakedQuad(quad.position0(), quad.position1(), quad.position2(), quad.position3(), quad.packedUV0(),
                quad.packedUV1(), quad.packedUV2(), quad.packedUV3(), quad.direction(), translucentMaterial);
    }
}
