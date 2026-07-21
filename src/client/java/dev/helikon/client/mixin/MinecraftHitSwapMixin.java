package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.MinecraftHitSwapAccess;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Selects HitSwap's weapon before vanilla reads the held item for its attack checks. */
@Mixin(Minecraft.class)
abstract class MinecraftHitSwapMixin {
    @Inject(
            method = "startAttack",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;",
                    ordinal = 0
            )
    )
    private void helikon$selectHitSwapBeforeAttackChecks(CallbackInfoReturnable<Boolean> callback) {
        MinecraftHitSwapAccess.beforeVanillaAttackChecks();
    }
}
