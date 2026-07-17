package dev.helikon.client.mixin;

import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the verified 26.2 {@code entityId} of an incoming relative-move packet without
 * touching world state, so BackTrack can match it against the current eligible-target set
 * on the network thread. The other position packets expose a public {@code id()} accessor.
 */
@Mixin(ClientboundMoveEntityPacket.class)
public interface ClientboundMoveEntityPacketAccessor {
    @Accessor("entityId")
    int helikon$entityId();
}
