package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.MinecraftBackTrackAccess;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets BackTrack defer an incoming entity position packet at the normal connection boundary.
 * When the module holds the packet the original delivery is cancelled; the module later
 * re-delivers it to the same listener on the client thread. Never modifies packet contents.
 */
@Mixin(Connection.class)
abstract class ConnectionBackTrackMixin {
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/protocol/Packet;)V",
            at = @At("HEAD"), cancellable = true)
    private void helikon$backTrackHold(ChannelHandlerContext context, Packet<?> packet, CallbackInfo callback) {
        if (MinecraftBackTrackAccess.intercept(packet, (Connection) (Object) this)) {
            callback.cancel();
        }
    }
}
