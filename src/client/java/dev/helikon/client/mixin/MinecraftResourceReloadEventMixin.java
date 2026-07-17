package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/** Observes the resource-pack reload boundary and re-enters the client thread for completion. */
@Mixin(Minecraft.class)
abstract class MinecraftResourceReloadEventMixin {
    @Inject(method = "reloadResourcePacks", at = @At("RETURN"))
    private void helikon$observeResourceReload(CallbackInfoReturnable<CompletableFuture<Void>> callback) {
        Minecraft client = (Minecraft) (Object) this;
        CompletableFuture<Void> reload = callback.getReturnValue();
        if (ClientEventAccess.beginResourceReload(reload)) {
            reload.whenComplete((unused, error) -> client.execute(
                    () -> ClientEventAccess.finishResourceReload(reload, error == null)));
        }
    }
}
