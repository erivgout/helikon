package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.MinecraftArrowDmgAccess;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Inserts ArrowDMG's bounded movement sequence immediately before an ordinary held-item release. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeArrowDmgMixin {
    @Inject(method = "releaseUsingItem", at = @At("HEAD"))
    private void helikon$beforeReleaseUsingItem(Player player, CallbackInfo callback) {
        MinecraftArrowDmgAccess.beforeRelease(player);
    }
}
