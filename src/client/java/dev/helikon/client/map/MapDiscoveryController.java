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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Bounded client-thread coordinator for loaded terrain discovery. */
public final class MapDiscoveryController {
    public static final int CAPTURES_PER_TICK = 4;
    public static final int RESEED_INTERVAL_TICKS = 40;
    public static final int MAXIMUM_TRACKED_CAPTURED_CHUNKS = 16384;

    private final Radar radar;
    private final WaypointLocationProvider locations;
    private final MapTileStore store;
    private final MinecraftMapChunkSampler sampler;
    private final MapCaptureQueue queue = new MapCaptureQueue();
    private final Set<ChunkCoordinate> capturedChunks = new LinkedHashSet<>();

    private WaypointContext context;
    private boolean captureWasActive;
    private boolean hasPrioritizedPlayerChunk;
    private int prioritizedPlayerChunkX;
    private int prioritizedPlayerChunkZ;
    private int observedStorageLimitMb = -1;
    private int ticksUntilReseed;

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
        capturedChunks.clear();
        context = null;
        captureWasActive = false;
        hasPrioritizedPlayerChunk = false;
        store.flush();
    }

    /** Samples a bounded batch of still-loaded queued chunks. */
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
            hasPrioritizedPlayerChunk = false;
            context = location == null ? null : location.context();
            return;
        }

        WaypointContext current = location.context();
        if (!current.equals(context) || !captureWasActive) {
            queue.clear();
            capturedChunks.clear();
            context = current;
            captureWasActive = true;
            hasPrioritizedPlayerChunk = false;
            ticksUntilReseed = RESEED_INTERVAL_TICKS;
            store.activate(current);
            seedLoadedChunks(location);
        }

        prioritizePlayerChunk(location);
        reseedIfDue(location);
        captureQueuedChunks(current);
    }

    /**
     * Loaded chunks past the queue bound are dropped at seed time, so the seed
     * sweep repeats until every chunk that stays loaded has been captured once.
     */
    private void reseedIfDue(WaypointLocation location) {
        if (--ticksUntilReseed > 0) {
            return;
        }
        ticksUntilReseed = RESEED_INTERVAL_TICKS;
        if (queue.size() < MapCaptureQueue.MAXIMUM_PENDING_CHUNKS) {
            seedLoadedChunks(location);
        }
    }

    private void captureQueuedChunks(WaypointContext current) {
        for (int attempt = 0; attempt < CAPTURES_PER_TICK; attempt++) {
            MapCaptureQueue.Entry entry = queue.peek().orElse(null);
            if (entry == null || !store.canAcceptUpdate()) {
                return;
            }
            if (!entry.context().equals(current)) {
                queue.remove(entry);
                continue;
            }
            Minecraft client = Minecraft.getInstance();
            if (client.level == null || !client.level.hasChunk(entry.chunkX(), entry.chunkZ())) {
                queue.remove(entry);
                continue;
            }
            MapChunkSnapshot snapshot = sampler.sample(current, entry.chunkX(), entry.chunkZ()).orElse(null);
            if (snapshot == null) {
                // A chunk that reports loaded but cannot sample must never pin
                // the queue head; a later reseed re-offers it if it comes back.
                queue.remove(entry);
                continue;
            }
            if (!store.submit(snapshot)) {
                return;
            }
            queue.remove(entry);
            markCaptured(entry.chunkX(), entry.chunkZ());
        }
    }

    private void markCaptured(int chunkX, int chunkZ) {
        ChunkCoordinate coordinate = new ChunkCoordinate(chunkX, chunkZ);
        capturedChunks.remove(coordinate);
        capturedChunks.add(coordinate);
        if (capturedChunks.size() > MAXIMUM_TRACKED_CAPTURED_CHUNKS) {
            Iterator<ChunkCoordinate> iterator = capturedChunks.iterator();
            iterator.next();
            iterator.remove();
        }
    }

    public int pendingChunks() {
        return queue.size();
    }

    private void prioritizePlayerChunk(WaypointLocation location) {
        int chunkX = location.x() >> 4;
        int chunkZ = location.z() >> 4;
        if (hasPrioritizedPlayerChunk
                && chunkX == prioritizedPlayerChunkX && chunkZ == prioritizedPlayerChunkZ) {
            return;
        }
        queue.prioritize(location.context(), chunkX, chunkZ);
        prioritizedPlayerChunkX = chunkX;
        prioritizedPlayerChunkZ = chunkZ;
        hasPrioritizedPlayerChunk = true;
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
                ChunkCoordinate coordinate = new ChunkCoordinate(chunkX, chunkZ);
                if (!capturedChunks.contains(coordinate) && client.level.hasChunk(chunkX, chunkZ)) {
                    loaded.add(coordinate);
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
