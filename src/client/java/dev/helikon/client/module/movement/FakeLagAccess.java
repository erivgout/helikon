package dev.helikon.client.module.movement;

import io.netty.channel.ChannelFutureListener;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

import java.util.List;
import java.util.Objects;

/**
 * Narrow bridge between the {@link FakeLag} module and Minecraft's connection boundary.
 * It withholds the local player's outgoing movement packets while the module is enabled
 * and releases them, in original order, either after the configured delay or immediately
 * when the module is disabled or the world is left.
 *
 * <p>Only {@link ServerboundMovePlayerPacket} traffic is held; keep-alive, chat, and every
 * other packet always passes straight through so the connection is never starved. Held
 * packets are re-sent verbatim through the same {@link Connection}; a per-thread guard lets
 * those re-sends bypass interception without reordering or duplication.
 */
public final class FakeLagAccess {
    private static final Object LOCK = new Object();
    private static final HeldPacketQueue<HeldPacket> QUEUE = new HeldPacketQueue<>();
    private static final ThreadLocal<Boolean> RELEASING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static volatile FakeLag module;

    private FakeLagAccess() {
    }

    public static void install(FakeLag fakeLag) {
        module = Objects.requireNonNull(fakeLag, "fakeLag");
    }

    /**
     * Called from the connection send boundary. Returns {@code true} when the packet was
     * captured and the caller must cancel the vanilla send; {@code false} lets the packet
     * flow normally.
     */
    public static boolean tryHold(Connection connection, Packet<?> packet, ChannelFutureListener listener,
                                  boolean flush) {
        FakeLag current = module;
        if (current == null || !current.isEnabled()) {
            return false;
        }
        if (RELEASING.get()) {
            return false;
        }
        if (connection == null || !(packet instanceof ServerboundMovePlayerPacket)) {
            return false;
        }
        List<HeldPacket> overflow;
        synchronized (LOCK) {
            overflow = QUEUE.enqueue(System.currentTimeMillis(),
                    new HeldPacket(connection, packet, listener, flush), current.maxHeldPackets());
        }
        release(overflow);
        return true;
    }

    /** Releases every held packet whose delay has elapsed. Invoked once per client tick. */
    public static void tick() {
        FakeLag current = module;
        if (current == null || !current.isEnabled()) {
            return;
        }
        List<HeldPacket> due;
        synchronized (LOCK) {
            due = QUEUE.drainReleasable(System.currentTimeMillis(), current.delayMillis());
        }
        release(due);
    }

    /** Releases every held packet immediately; used when the module is disabled or panic runs. */
    public static void flushAll() {
        List<HeldPacket> all;
        synchronized (LOCK) {
            all = QUEUE.drainAll();
        }
        release(all);
    }

    /** Discards held packets without sending them; used when the connection is gone. */
    public static void reset() {
        synchronized (LOCK) {
            QUEUE.clear();
        }
    }

    private static void release(List<HeldPacket> packets) {
        if (packets.isEmpty()) {
            return;
        }
        RELEASING.set(Boolean.TRUE);
        try {
            for (HeldPacket held : packets) {
                held.connection().send(held.packet(), held.listener(), held.flush());
            }
        } finally {
            RELEASING.set(Boolean.FALSE);
        }
    }

    private record HeldPacket(Connection connection, Packet<?> packet, ChannelFutureListener listener, boolean flush) {
        private HeldPacket {
            Objects.requireNonNull(connection, "connection");
            Objects.requireNonNull(packet, "packet");
        }
    }
}
