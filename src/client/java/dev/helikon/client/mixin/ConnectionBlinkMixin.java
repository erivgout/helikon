package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.BlinkPacketAccess;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Cancels only outgoing player-movement sends that Blink chooses to hold. */
@Mixin(Connection.class)
abstract class ConnectionBlinkMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void helikon$holdMovementPacket(Packet<?> packet, ChannelFutureListener listener, boolean flush,
                                            CallbackInfo callback) {
        if (BlinkPacketAccess.holdOutgoing(packet)) {
            callback.cancel();
        }
    }
}
