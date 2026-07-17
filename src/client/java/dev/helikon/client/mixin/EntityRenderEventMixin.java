package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import dev.helikon.client.event.RenderEvent;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes extraction of each locally rendered entity without retaining entity state. */
@Mixin(EntityRenderDispatcher.class)
abstract class EntityRenderEventMixin {
    @Inject(method = "extractEntity", at = @At("HEAD"))
    private <E extends Entity> void helikon$observeEntityRender(E entity, float partialTick,
                                                                  CallbackInfoReturnable<?> callback) {
        ClientEventAccess.postRender(RenderEvent.Kind.ENTITY, partialTick,
                BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
    }
}
