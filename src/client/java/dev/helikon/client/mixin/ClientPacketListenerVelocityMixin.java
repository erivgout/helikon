package dev.helikon.client.mixin;

import dev.helikon.client.module.combat.MinecraftVelocityAccess;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Alters only the local player's argument to Minecraft's verified 26.2 motion-packet handler. */
@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerVelocityMixin {
    @ModifyVariable(method = "handleSetEntityMotion", at = @At("HEAD"), argsOnly = true)
    private ClientboundSetEntityMotionPacket helikon$scaleLocalPlayerMotion(
            ClientboundSetEntityMotionPacket packet
    ) {
        return MinecraftVelocityAccess.adjust(packet);
    }
}
