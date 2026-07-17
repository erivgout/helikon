package dev.helikon.client.mixin;

import dev.helikon.client.event.ClientEventAccess;
import dev.helikon.client.event.PacketObservationEvent;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Captures packet-class metadata at the normal connection boundary only. */
@Mixin(Connection.class)
abstract class ConnectionPacketObservationMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"))
    private void helikon$observePacketSend(Packet<?> packet, ChannelFutureListener listener, boolean flush,
                                           CallbackInfo callback) {
        ClientEventAccess.postPacket(PacketObservationEvent.Direction.SEND, packet.getClass().getName());
    }

    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"))
    private void helikon$observePacketReceive(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        ClientEventAccess.postPacket(PacketObservationEvent.Direction.RECEIVE, packet.getClass().getName());
    }
}
