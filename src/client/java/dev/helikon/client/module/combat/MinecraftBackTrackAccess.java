package dev.helikon.client.module.combat;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.entity.MinecraftEntityClassification;
import dev.helikon.client.mixin.ClientboundMoveEntityPacketAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

/**
 * Narrow 26.2 bridge for BackTrack. The client thread publishes which loaded entity ids are
 * currently eligible and drains due held packets; the network thread only reads that snapshot
 * and defers matching packets. Held packets are always re-dispatched on the client thread via
 * {@link #dispatch(Packet, PacketListener)}, mirroring Minecraft's own delivery path.
 */
public final class MinecraftBackTrackAccess {
    private static final int MAX_HELD_PACKETS = 4096;

    /** A deferred clientbound packet plus the listener that would have handled it. */
    private record Held(Packet<?> packet, PacketListener listener) {
    }

    /** Immutable snapshot the network thread reads without touching Minecraft state. */
    private record State(boolean active, long delayMillis, Set<Integer> eligibleIds) {
        static final State INACTIVE = new State(false, 0L, Set.of());
    }

    private static final Object LOCK = new Object();
    private static final BackTrackBuffer<Held> BUFFER = new BackTrackBuffer<>(MAX_HELD_PACKETS);
    private static volatile State state = State.INACTIVE;

    private MinecraftBackTrackAccess() {
    }

    /**
     * Runs once per client tick on the client thread: refreshes the eligible-id snapshot, releases
     * held packets whose delay elapsed, and flushes everything the moment the module is inactive so
     * no entity is left frozen at a stale position.
     */
    public static void tick(BackTrack module, FriendManager friends) {
        Objects.requireNonNull(module, "module");
        Objects.requireNonNull(friends, "friends");
        Minecraft client = Minecraft.getInstance();
        long now = System.currentTimeMillis();
        if (client.player == null || client.level == null) {
            // The connection these packets belonged to is gone; drop them rather than
            // replay stale positions into a new world.
            synchronized (LOCK) {
                BUFFER.clear();
            }
            state = State.INACTIVE;
            return;
        }
        if (!module.isEnabled()) {
            state = State.INACTIVE;
            releaseAll();
            return;
        }
        Set<Integer> eligible = eligibleIds(client.level, client.player, friends, module);
        state = new State(true, module.delayMillis(), eligible);
        releaseDue(now);
    }

    /**
     * Network-thread entry point. Returns {@code true} when the packet was held (the caller must then
     * cancel normal delivery); {@code false} to let Minecraft deliver it as usual. Reads only the
     * published snapshot and a primitive entity id from the packet, never live world state.
     */
    public static boolean intercept(Packet<?> packet, Connection connection) {
        State current = state;
        if (!current.active() || packet == null || connection == null) {
            return false;
        }
        OptionalInt id = entityId(packet);
        if (id.isEmpty() || !current.eligibleIds().contains(id.getAsInt())) {
            return false;
        }
        PacketListener listener = connection.getPacketListener();
        if (listener == null) {
            return false;
        }
        long releaseAt = System.currentTimeMillis() + current.delayMillis();
        synchronized (LOCK) {
            return BUFFER.enqueue(new Held(packet, listener), releaseAt);
        }
    }

    /** Discards every held packet and resets the snapshot; used on disconnect and panic-style teardown. */
    public static void reset() {
        synchronized (LOCK) {
            BUFFER.clear();
        }
        state = State.INACTIVE;
    }

    private static void releaseDue(long now) {
        List<Held> due;
        synchronized (LOCK) {
            due = BUFFER.drainDue(now);
        }
        due.forEach(MinecraftBackTrackAccess::deliver);
    }

    private static void releaseAll() {
        List<Held> all;
        synchronized (LOCK) {
            all = BUFFER.drainAll();
        }
        all.forEach(MinecraftBackTrackAccess::deliver);
    }

    private static void deliver(Held held) {
        dispatch(held.packet(), held.listener());
    }

    /**
     * Re-delivers a previously held packet to its listener. This is only ever called on the client
     * thread, where the handler's same-thread guard passes and the position update applies normally.
     */
    @SuppressWarnings("unchecked")
    private static <T extends PacketListener> void dispatch(Packet<T> packet, PacketListener listener) {
        packet.handle((T) listener);
    }

    private static OptionalInt entityId(Packet<?> packet) {
        if (packet instanceof ClientboundMoveEntityPacket move) {
            return OptionalInt.of(((ClientboundMoveEntityPacketAccessor) move).helikon$entityId());
        }
        if (packet instanceof ClientboundTeleportEntityPacket teleport) {
            return OptionalInt.of(teleport.id());
        }
        if (packet instanceof ClientboundEntityPositionSyncPacket sync) {
            return OptionalInt.of(sync.id());
        }
        if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            return OptionalInt.of(motion.id());
        }
        return OptionalInt.empty();
    }

    private static Set<Integer> eligibleIds(ClientLevel level, LocalPlayer player, FriendManager friends, BackTrack module) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof LivingEntity living) || living == player) {
                continue;
            }
            boolean isPlayer = living instanceof Player;
            boolean isHostile = MinecraftEntityClassification.isHostile(living);
            boolean friend = isPlayer && isFriend((Player) living, friends);
            double distance = Math.sqrt(player.distanceToSqr(living));
            if (module.shouldDelay(isPlayer, isHostile, friend, distance)) {
                ids.add(living.getId());
            }
        }
        return Set.copyOf(ids);
    }

    private static boolean isFriend(Player player, FriendManager friends) {
        String name = player.getGameProfile().name();
        return name != null && !name.isBlank() && friends.contains(name.trim());
    }
}
