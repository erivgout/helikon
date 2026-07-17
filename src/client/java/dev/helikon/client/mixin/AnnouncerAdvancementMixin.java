package dev.helikon.client.mixin;

import dev.helikon.client.module.chat.AnnouncerAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateAdvancementsPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Observes this client's completed advancement progress after Minecraft accepts the normal packet. */
@Mixin(ClientPacketListener.class)
abstract class AnnouncerAdvancementMixin {
    @Inject(method = "handleUpdateAdvancementsPacket", at = @At("TAIL"))
    private void helikon$observeCompletedAdvancements(ClientboundUpdateAdvancementsPacket packet, CallbackInfo callback) {
        packet.getProgress().forEach((id, progress) -> {
            if (progress.isDone()) {
                AnnouncerAccess.observeAdvancement(id.toString());
            }
        });
    }
}
