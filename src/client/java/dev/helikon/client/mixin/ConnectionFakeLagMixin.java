package dev.helikon.client.mixin;

import dev.helikon.client.module.movement.FakeLagAccess;
import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets FakeLag withhold the local player's outgoing movement packets at the single send
 * funnel every overload routes through. When the module captures a packet the vanilla send
 * is cancelled; FakeLag re-sends it later verbatim through the same connection.
 */
@Mixin(Connection.class)
abstract class ConnectionFakeLagMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void helikon$holdOutgoingPacket(Packet<?> packet, ChannelFutureListener listener, boolean flush,
                                            CallbackInfo callback) {
        if (FakeLagAccess.tryHold((Connection) (Object) this, packet, listener, flush)) {
            callback.cancel();
        }
    }
}
