package dev.helikon.client.module.render;

import dev.helikon.client.friend.FriendManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Reversible local camera bridge; it never moves an entity or constructs network traffic. */
public final class RemoteViewAccess {
    private static final int MAXIMUM_OBSERVED_ENTITIES = 1_024;

    private static volatile RemoteView remoteView;
    private static volatile FriendManager friends;
    private static Entity target;
    private static Entity previousCamera;
    private static boolean wasEnabled;
    private static boolean waitingAfterLoss;
    private static long observedSelectionRevision = Long.MIN_VALUE;

    private RemoteViewAccess() {
    }

    public static void install(RemoteView module, FriendManager friendManager) {
        remoteView = Objects.requireNonNull(module, "module");
        friends = Objects.requireNonNull(friendManager, "friendManager");
        module.setViewRestorer(() -> stop(Minecraft.getInstance()));
    }

    public static void tick(Minecraft client) {
        Objects.requireNonNull(client, "client");
        RemoteView current = remoteView;
        if (current == null || !current.isEnabled() || client.level == null || client.player == null) {
            stop(client);
            return;
        }
        if (!wasEnabled) {
            wasEnabled = true;
            waitingAfterLoss = false;
            observedSelectionRevision = current.selectionRevision();
        } else if (observedSelectionRevision != current.selectionRevision()) {
            restoreCamera(client);
            waitingAfterLoss = false;
            observedSelectionRevision = current.selectionRevision();
        }
        if (target != null && !isCurrentTarget(client, target)) {
            restoreCamera(client);
            waitingAfterLoss = !current.retargetOnLoss();
        }
        if (target == null && !waitingAfterLoss) {
            acquireTarget(client, current);
        }
        if (target != null && client.getCameraEntity() != target) {
            client.setCameraEntity(target);
        }
    }

    /** Restores the prior valid camera and forgets all transient target state. */
    public static void stop(Minecraft client) {
        Objects.requireNonNull(client, "client");
        restoreCamera(client);
        wasEnabled = false;
        waitingAfterLoss = false;
        observedSelectionRevision = Long.MIN_VALUE;
    }

    private static void acquireTarget(Minecraft client, RemoteView current) {
        List<RemoteView.Candidate> candidates = new ArrayList<>();
        Entity crosshair = client.crosshairPickEntity;
        int observed = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player) {
                continue;
            }
            candidates.add(candidate(client, entity));
            if (++observed >= MAXIMUM_OBSERVED_ENTITIES) {
                break;
            }
        }
        if (crosshair != null && crosshair != client.player
                && candidates.stream().noneMatch(candidate -> candidate.entityId() == crosshair.getId())) {
            candidates.add(candidate(client, crosshair));
        }
        Integer crosshairId = crosshair == null ? null : crosshair.getId();
        Optional<RemoteView.Candidate> selected = current.select(candidates, crosshairId);
        if (selected.isEmpty()) {
            return;
        }
        Entity selectedEntity = client.level.getEntity(selected.get().entityId());
        if (selectedEntity == null || selectedEntity == client.player || selectedEntity.isRemoved()) {
            return;
        }
        previousCamera = client.getCameraEntity();
        target = selectedEntity;
        client.setCameraEntity(target);
    }

    private static RemoteView.Candidate candidate(Minecraft client, Entity entity) {
        String typeId = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        String name = entity instanceof Player player
                ? player.getGameProfile().name()
                : entity.getName().getString();
        boolean friend = entity instanceof Player player && friends.contains(player.getGameProfile().name());
        boolean spectator = entity instanceof Player player && player.isSpectator();
        return new RemoteView.Candidate(entity.getId(), typeId, name,
                entity.position().distanceToSqr(client.player.position()), friend,
                entity.isInvisibleTo(client.player), spectator);
    }

    private static boolean isCurrentTarget(Minecraft client, Entity entity) {
        ClientLevel level = client.level;
        return level != null && !entity.isRemoved() && entity.level() == level
                && level.getEntity(entity.getId()) == entity;
    }

    private static void restoreCamera(Minecraft client) {
        if (target != null && client.getCameraEntity() == target) {
            Entity restore = validPreviousCamera(client) ? previousCamera : client.player;
            if (restore != null) {
                client.setCameraEntity(restore);
            }
        }
        target = null;
        previousCamera = null;
    }

    private static boolean validPreviousCamera(Minecraft client) {
        return previousCamera != null && !previousCamera.isRemoved()
                && (previousCamera == client.player
                || client.level != null && previousCamera.level() == client.level);
    }
}
