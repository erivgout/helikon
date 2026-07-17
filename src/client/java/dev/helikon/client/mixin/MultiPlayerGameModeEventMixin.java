package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import dev.helikon.client.event.InteractionEvent;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Observes completed normal local game-mode interactions; it never changes their result. */
@Mixin(MultiPlayerGameMode.class)
abstract class MultiPlayerGameModeEventMixin {
    @Inject(method = "attack", at = @At("RETURN"))
    private void helikon$observeAttack(Player player, Entity target, CallbackInfo callback) {
        if (player instanceof LocalPlayer) {
            ClientEventAccess.postInteraction(InteractionEvent.Kind.ATTACK, entityId(target));
        }
    }

    @Inject(method = "useItem", at = @At("RETURN"))
    private void helikon$observeItemUse(Player player, InteractionHand hand,
                                        CallbackInfoReturnable<InteractionResult> callback) {
        if (player instanceof LocalPlayer && callback.getReturnValue().consumesAction()) {
            ClientEventAccess.postInteraction(InteractionEvent.Kind.ITEM_USE, itemId(player.getItemInHand(hand)));
        }
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void helikon$observeBlockPlacement(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
                                                CallbackInfoReturnable<InteractionResult> callback) {
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof BlockItem && callback.getReturnValue().consumesAction()) {
            ClientEventAccess.postInteraction(InteractionEvent.Kind.BLOCK_PLACE, itemId(held));
        }
    }

    private static String entityId(Entity entity) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
    }

    private static String itemId(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }
}
