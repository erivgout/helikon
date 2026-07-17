package dev.helikon.client.mixin;

import dev.helikon.client.module.chat.AnnouncementTrigger;
import dev.helikon.client.module.chat.AnnouncerAccess;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes only successful local pickup reductions after ItemEntity's normal client handling. */
@Mixin(ItemEntity.class)
abstract class AnnouncerItemEntityMixin {
    @Unique
    private int helikon$initialItemCount = -1;

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void helikon$rememberInitialCount(Player player, CallbackInfo callback) {
        if (player == Minecraft.getInstance().player) {
            helikon$initialItemCount = ((ItemEntity) (Object) this).getItem().getCount();
        }
    }

    @Inject(method = "playerTouch", at = @At("TAIL"))
    private void helikon$observeLocalPickup(Player player, CallbackInfo callback) {
        ItemEntity item = (ItemEntity) (Object) this;
        if (player == Minecraft.getInstance().player && helikon$initialItemCount > item.getItem().getCount()) {
            AnnouncerAccess.enqueue(AnnouncementTrigger.ITEM_PICKUP,
                    BuiltInRegistries.ITEM.getKey(item.getItem().getItem()).toString());
        }
        helikon$initialItemCount = -1;
    }
}
