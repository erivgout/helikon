package dev.helikon.client.map;

import dev.helikon.client.event.BlockChangeEvent;
import dev.helikon.client.event.ChunkEvent;
import dev.helikon.client.module.render.Radar;
import dev.helikon.client.waypoint.WaypointContext;
import dev.helikon.client.waypoint.WaypointLocation;
import dev.helikon.client.waypoint.WaypointLocationProvider;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Bounded client-thread coordinator for loaded terrain discovery. */
public final class MapDiscoveryController {
    private final Radar radar;
    private final WaypointLocationProvider locations;
    private final MapTileStore store;
    private final MinecraftMapChunkSampler sampler;
    private final MapCaptureQueue queue = new MapCaptureQueue();

    private WaypointContext context;
    private boolean captureWasActive;
    private int observedStorageLimitMb = -1;

    public MapDiscoveryController(Radar radar, WaypointLocationProvider locations, MapTileStore store) {
        this(radar, locations, store, new MinecraftMapChunkSampler());
    }

    MapDiscoveryController(Radar radar, WaypointLocationProvider locations, MapTileStore store,
                           MinecraftMapChunkSampler sampler) {
        this.radar = Objects.requireNonNull(radar, "radar");
        this.locations = Objects.requireNonNull(locations, "locations");
        this.store = Objects.requireNonNull(store, "store");
        this.sampler = Objects.requireNonNull(sampler, "sampler");
    }

    public void observeChunk(ChunkEvent event) {
        Objects.requireNonNull(event, "event");
        if (event.phase() == ChunkEvent.Phase.LOAD && captureActive() && context != null) {
            queue.offer(context, event.chunkX(), event.chunkZ());
        }
    }

    public void observeBlockChange(BlockChangeEvent event) {
        Objects.requireNonNull(event, "event");
        if (captureActive() && context != null) {
            queue.offer(context, event.x() >> 4, event.z() >> 4);
        }
    }

    public void onWorldChange() {
        queue.clear();
        context = null;
        captureWasActive = false;
        store.flush();
    }

    /** Samples no more than one still-loaded queued chunk. */
    public void tick() {
        WaypointLocation location = locations.currentLocation().orElse(null);
        int storageLimitMb = radar.mapStorageLimitMb();
        if (storageLimitMb != observedStorageLimitMb
                && store.status().state() == MapStoreStatus.State.QUOTA_REACHED) {
            store.recheckQuota();
        }
        observedStorageLimitMb = storageLimitMb;
        boolean active = captureActive() && location != null;
        if (!active) {
            if (captureWasActive || context != null) {
                queue.clear();
            }
            captureWasActive = false;
            context = location == null ? null : location.context();
            return;
        }

        WaypointContext current = location.context();
        if (!current.equals(context) || !captureWasActive) {
            queue.clear();
            context = current;
            captureWasActive = true;
            store.activate(current);
            seedLoadedChunks(location);
        }

        MapCaptureQueue.Entry entry = queue.peek().orElse(null);
        if (entry == null || !store.canAcceptUpdate()) {
            return;
        }
        if (!entry.context().equals(current)) {
            queue.remove(entry);
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || !client.level.hasChunk(entry.chunkX(), entry.chunkZ())) {
            queue.remove(entry);
            return;
        }
        sampler.sample(current, entry.chunkX(), entry.chunkZ()).ifPresent(snapshot -> {
            if (store.submit(snapshot)) {
                queue.remove(entry);
            }
        });
    }

    public int pendingChunks() {
        return queue.size();
    }

    private void seedLoadedChunks(WaypointLocation location) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        int centerChunkX = location.x() >> 4;
        int centerChunkZ = location.z() >> 4;
        int radius = Math.clamp(client.options.renderDistance().get(), 2, 32);
        List<ChunkCoordinate> loaded = new ArrayList<>();
        for (int chunkZ = centerChunkZ - radius; chunkZ <= centerChunkZ + radius; chunkZ++) {
            for (int chunkX = centerChunkX - radius; chunkX <= centerChunkX + radius; chunkX++) {
                if (client.level.hasChunk(chunkX, chunkZ)) {
                    loaded.add(new ChunkCoordinate(chunkX, chunkZ));
                }
            }
        }
        loaded.sort(Comparator.comparingLong(coordinate -> coordinate.distanceSquared(centerChunkX, centerChunkZ)));
        for (ChunkCoordinate coordinate : loaded) {
            if (!queue.offer(location.context(), coordinate.x(), coordinate.z())
                    && queue.size() >= MapCaptureQueue.MAXIMUM_PENDING_CHUNKS) {
                break;
            }
        }
    }

    private boolean captureActive() {
        return radar.isEnabled() && radar.minimap() && radar.saveDiscoveredMap();
    }

    private record ChunkCoordinate(int x, int z) {
        private long distanceSquared(int centerX, int centerZ) {
            long deltaX = (long) x - centerX;
            long deltaZ = (long) z - centerZ;
            return deltaX * deltaX + deltaZ * deltaZ;
        }
    }
}
