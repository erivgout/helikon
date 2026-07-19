package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.entity.MinecraftEntityClassification;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies the vanilla upside-down transform only to locally selected living-entity categories. */
@Mixin(targets = "net.minecraft.client.renderer.entity.LivingEntityRenderer")
abstract class LivingEntityRendererMixin {
    @Inject(method = "isEntityUpsideDown", at = @At("RETURN"), cancellable = true)
    private void helikon$flipSelectedEntities(LivingEntity entity, CallbackInfoReturnable<Boolean> callback) {
        if (RenderModuleAccess.shouldFlipDinnerbone(entityType(entity))) {
            callback.setReturnValue(true);
        }
    }

    private static EntityRenderFilter.EntityType entityType(LivingEntity entity) {
        if (entity instanceof Player) {
            return EntityRenderFilter.EntityType.PLAYER;
        }
        if (MinecraftEntityClassification.isHostile(entity)) {
            return EntityRenderFilter.EntityType.HOSTILE;
        }
        return EntityRenderFilter.EntityType.PASSIVE;
    }
}
