package dev.helikon.client.mixin;

import dev.helikon.client.module.render.RenderModuleAccess;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.item.ShieldItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Makes only a raised local vanilla shield use the ordinary held-item render path. */
@Mixin(targets = "net.minecraft.client.renderer.ItemInHandRenderer")
abstract class ItemInHandRendererMixin {
    @Redirect(
            method = "submitArmWithItem",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z", ordinal = 1)
    )
    private boolean helikon$hideRaisedShield(AbstractClientPlayer player) {
        boolean usingItem = player.isUsingItem();
        boolean usingShield = usingItem && player.getUseItem().getItem() instanceof ShieldItem;
        return usingItem && !RenderModuleAccess.hideRaisedShield(usingItem, usingShield);
    }
}
