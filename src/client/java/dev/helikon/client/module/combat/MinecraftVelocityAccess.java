package dev.helikon.client.module.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Narrow 26.2 adapter for the vanilla local-player entity-motion packet handler. */
public final class MinecraftVelocityAccess {
    private static volatile Velocity module;

    private MinecraftVelocityAccess() {
    }

    public static void install(Velocity velocity) {
        module = Objects.requireNonNull(velocity, "velocity");
    }

    /**
     * Rebuilds only a local-player packet with its original vanilla packet type and entity ID.
     * Motion packets for every other entity are returned by identity.
     */
    public static ClientboundSetEntityMotionPacket adjust(ClientboundSetEntityMotionPacket packet) {
        Velocity current = module;
        LocalPlayer player = Minecraft.getInstance().player;
        if (current == null || player == null || packet.id() != player.getId()) {
            return packet;
        }

        Vec3 received = packet.movement();
        Velocity.Motion adjusted = current.adjust(received.x(), received.y(), received.z());
        if (adjusted.x() == received.x() && adjusted.y() == received.y() && adjusted.z() == received.z()) {
            return packet;
        }
        return new ClientboundSetEntityMotionPacket(
                packet.id(),
                new Vec3(adjusted.x(), adjusted.y(), adjusted.z())
        );
    }
}
