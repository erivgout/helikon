package dev.helikon.client.module.movement;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;
import java.util.Objects;

/**
 * Narrow bridge between Minecraft's ordinary connection send path and Blink's
 * pure hold/release policy. The connection mixin asks whether an outgoing
 * movement packet should be held; the client tick releases the hold when the
 * module is disabled or its safety cap is reached. Released packets travel back
 * through Minecraft's normal {@code ClientPacketListener.send} path, so the
 * server applies its usual validation.
 */
public final class BlinkPacketAccess {
    private static volatile Blink module;
    private static final BlinkBuffer<Packet<?>> BUFFER = new BlinkBuffer<>();
    private static volatile boolean releasing;

    private BlinkPacketAccess() {
    }

    public static void install(Blink blink) {
        module = Objects.requireNonNull(blink, "blink");
    }

    /**
     * Called from the connection mixin at the head of an outgoing send. Returns
     * {@code true} when the packet was buffered and its send must be cancelled.
     */
    public static boolean holdOutgoing(Packet<?> packet) {
        Blink current = module;
        if (current == null || releasing || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        if (current.shouldHold(BUFFER.size())) {
            BUFFER.add(packet);
            return true;
        }
        return false;
    }

    /** Releases or discards the hold once per client tick as the policy requires. */
    public static void tick() {
        Blink current = module;
        if (current == null || !current.shouldRelease(BUFFER.size())) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        if (player == null || player.connection == null) {
            BUFFER.clear();
            return;
        }
        release(player);
    }

    /** Discards any held packets without sending them; used when the world is left. */
    public static void discard() {
        BUFFER.clear();
    }

    private static void release(LocalPlayer player) {
        List<Packet<?>> pending = BUFFER.drain();
        if (pending.isEmpty()) {
            return;
        }
        releasing = true;
        try {
            for (Packet<?> packet : pending) {
                player.connection.send(packet);
            }
        } finally {
            releasing = false;
        }
    }
}
