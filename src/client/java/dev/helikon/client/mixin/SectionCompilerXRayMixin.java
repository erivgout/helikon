package dev.helikon.client.mixin;

import dev.helikon.client.module.render.XRayRenderAccess;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Filters only local chunk-model compilation while XRay is enabled. */
@Mixin(SectionCompiler.class)
abstract class SectionCompilerXRayMixin {
    @Inject(method = "compile", at = @At("HEAD"))
    private void helikon$beginXRayCompilation(SectionPos sectionPos, RenderSectionRegion region, VertexSorting sorting,
                                               SectionBufferBuilderPack buffers,
                                               CallbackInfoReturnable<SectionCompiler.Results> callback) {
        XRayRenderAccess.beginChunkCompilation();
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private void helikon$endXRayCompilation(SectionPos sectionPos, RenderSectionRegion region, VertexSorting sorting,
                                             SectionBufferBuilderPack buffers,
                                             CallbackInfoReturnable<SectionCompiler.Results> callback) {
        XRayRenderAccess.endChunkCompilation();
    }

    @Redirect(method = "compile", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"))
    private boolean helikon$hideNonTargetBlockModels(BlockState state) {
        boolean air = state.isAir();
        if (air || !XRayRenderAccess.isCompilingXRayChunk()) {
            return air;
        }
        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        return XRayRenderAccess.hides(blockId);
    }
}
