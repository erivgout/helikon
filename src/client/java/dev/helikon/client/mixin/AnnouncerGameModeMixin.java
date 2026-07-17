package dev.helikon.client.mixin;

import dev.helikon.client.module.chat.AnnouncerAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Remembers ordinary local melee attempts without altering Minecraft's attack path. */
@Mixin(MultiPlayerGameMode.class)
abstract class AnnouncerGameModeMixin {
    @Inject(method = "attack", at = @At("TAIL"))
    private void helikon$observeLocalAttack(Player player, Entity target, CallbackInfo callback) {
        if (player == Minecraft.getInstance().player) {
            AnnouncerAccess.recordAttack(target.getUUID(), target.getName().getString(), System.currentTimeMillis());
        }
    }
}
