package dev.helikon.client.mixin;

import dev.helikon.client.module.miscellaneous.MinecraftKnockbackDelayAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Delays only local-player entity motion after Minecraft has handed packet work to the client thread. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerKnockbackDelayMixin {
    @Inject(
            method = "handleSetEntityMotion",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread("
                            + "Lnet/minecraft/network/protocol/Packet;"
                            + "Lnet/minecraft/network/PacketListener;"
                            + "Lnet/minecraft/network/PacketProcessor;)V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    private void helikon$delayLocalPlayerMotion(ClientboundSetEntityMotionPacket packet, CallbackInfo callback) {
        if (MinecraftKnockbackDelayAccess.delay(packet)) {
            callback.cancel();
        }
    }
}
