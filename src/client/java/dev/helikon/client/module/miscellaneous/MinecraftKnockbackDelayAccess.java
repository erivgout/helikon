package dev.helikon.client.module.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** Narrow 26.2 adapter for the vanilla local-player entity-motion packet handler. */
public final class MinecraftKnockbackDelayAccess {
    private static volatile KnockbackDelay module;
    private static volatile long observedClientTick;

    private MinecraftKnockbackDelayAccess() {
    }

    public static void install(KnockbackDelay knockbackDelay) {
        module = Objects.requireNonNull(knockbackDelay, "knockbackDelay");
        observedClientTick = 0L;
    }

    /**
     * Returns true only when a local-player packet was safely retained for delayed application.
     * Packets for other entities, disabled/full queues, and unavailable players stay on vanilla's path.
     */
    public static boolean delay(ClientboundSetEntityMotionPacket packet) {
        KnockbackDelay current = module;
        LocalPlayer player = Minecraft.getInstance().player;
        if (current == null || player == null || packet.id() != player.getId()) {
            return false;
        }
        Vec3 motion = packet.movement();
        return current.delay(observedClientTick, new KnockbackDelay.Motion(motion.x(), motion.y(), motion.z()));
    }

    /** Applies due motion through the same Entity.lerpMotion call used by the verified vanilla handler. */
    public static void tick(long clientTick) {
        observedClientTick = clientTick;
        KnockbackDelay current = module;
        if (current == null) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            current.clearPending();
            return;
        }
        current.releaseReady(clientTick).forEach(MinecraftKnockbackDelayAccess::apply);
    }

    /** Clears delayed motion when the corresponding world/player is no longer valid. */
    public static void reset() {
        observedClientTick = 0L;
        KnockbackDelay current = module;
        if (current != null) {
            current.clearPending();
        }
    }

    public static void apply(KnockbackDelay.Motion motion) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.lerpMotion(new Vec3(motion.x(), motion.y(), motion.z()));
        }
    }
}
