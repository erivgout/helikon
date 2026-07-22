package dev.helikon.client.map;

import dev.helikon.client.waypoint.WaypointContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntSupplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Single-worker persistent map store. Minecraft-facing callers submit immutable
 * snapshots and read immutable published regions without blocking on disk.
 */
public final class MapTileStore implements AutoCloseable {
    public static final int MAXIMUM_QUEUED_UPDATES = 64;
    public static final int MAXIMUM_CACHED_REGIONS = 64;
    public static final int MAXIMUM_PUBLISHED_REGIONS = 128;
    private static final int FLUSH_CHANGE_THRESHOLD = 16;
    private static final long FLUSH_DELAY_SECONDS = 5L;
    private static final Logger LOGGER = Logger.getLogger(MapTileStore.class.getName());

    private final Path mapsRoot;
    private final IntSupplier limitMegabytes;
    private final MapRegionCodec codec;
    private final ScheduledThreadPoolExecutor worker;
    private final Map<RegionKey, CachedRegion> regions = new LinkedHashMap<>(16, 0.75F, true);
    private final Map<RegionKey, MapRegion.Snapshot> published = new ConcurrentHashMap<>();
    private final Map<RegionKey, Boolean> publishedOrder = new LinkedHashMap<>(16, 0.75F, true);
    private final Set<RegionKey> requestedLoads = ConcurrentHashMap.newKeySet();
    private final Set<RegionKey> missingRegions = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pendingUpdates = new AtomicInteger();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean quotaRecheckPending = new AtomicBoolean();

    private volatile WaypointContext activeContext;
    private volatile MapStoreStatus status = new MapStoreStatus(MapStoreStatus.State.LOADING,
            "Map storage is starting");
    private long diskBytes;
    private boolean flushScheduled;

    public MapTileStore(Path mapsRoot, IntSupplier limitMegabytes) {
        this(mapsRoot, limitMegabytes, new MapRegionCodec());
    }

    MapTileStore(Path mapsRoot, IntSupplier limitMegabytes, MapRegionCodec codec) {
        this.mapsRoot = Objects.requireNonNull(mapsRoot, "mapsRoot").toAbsolutePath().normalize();
        this.limitMegabytes = Objects.requireNonNull(limitMegabytes, "limitMegabytes");
        this.codec = Objects.requireNonNull(codec, "codec");
        worker = new ScheduledThreadPoolExecutor(1, runnable -> {
            Thread thread = new Thread(runnable, "Helikon map storage");
            thread.setDaemon(true);
            return thread;
        });
        worker.setRemoveOnCancelPolicy(true);
        worker.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
    }

    public MapStoreStatus status() {
        return status;
    }

    public WaypointContext activeContext() {
        return activeContext;
    }

    public boolean canAcceptUpdate() {
        return !closed.get() && status.state() == MapStoreStatus.State.READY
                && pendingUpdates.get() < MAXIMUM_QUEUED_UPDATES;
    }

    /** Switches contexts after flushing the old cache on the storage worker. */
    public void activate(WaypointContext context) {
        Objects.requireNonNull(context, "context");
        if (closed.get() || context.equals(activeContext)) {
            return;
        }
        activeContext = context;
        status = new MapStoreStatus(MapStoreStatus.State.LOADING, "Loading local map");
        submitWorker(() -> switchContext(context));
    }

    public boolean submit(MapChunkSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        if (!canAcceptUpdate() || !snapshot.context().equals(activeContext)) {
            return false;
        }
        int queued = pendingUpdates.incrementAndGet();
        if (queued > MAXIMUM_QUEUED_UPDATES) {
            pendingUpdates.decrementAndGet();
            return false;
        }
        try {
            worker.execute(() -> {
                try {
                    apply(snapshot);
                } finally {
                    pendingUpdates.decrementAndGet();
                }
            });
            return true;
        } catch (RejectedExecutionException exception) {
            pendingUpdates.decrementAndGet();
            return false;
        }
    }

    /** Starts an asynchronous load if this visible region is not published. */
    public void request(WaypointContext context, int regionX, int regionZ) {
        RegionKey key = new RegionKey(context, regionX, regionZ);
        MapStoreStatus.State currentState = status.state();
        if (closed.get() || currentState == MapStoreStatus.State.CLOSED
                || currentState == MapStoreStatus.State.IO_ERROR
                || currentState == MapStoreStatus.State.UNSUPPORTED_VERSION
                || !context.equals(activeContext) || published.containsKey(key)
                || missingRegions.contains(key)
                || requestedLoads.size() >= MAXIMUM_CACHED_REGIONS || !requestedLoads.add(key)) {
            return;
        }
        submitWorker(() -> {
            try {
                MapStorageKey storage = new MapStorageKey(mapsRoot, key.context());
                Path primary = storage.regionPath(key.regionX(), key.regionZ());
                if (Files.notExists(primary) && Files.notExists(MapRegionCodec.backupPath(primary))) {
                    if (missingRegions.size() < MAXIMUM_PUBLISHED_REGIONS) {
                        missingRegions.add(key);
                    }
                    return;
                }
                CachedRegion region = region(key);
                publish(key, region.region.snapshot());
            } catch (MapRegionCodec.UnsupportedVersionException
                     | MapStorageKey.UnsupportedMetadataVersionException exception) {
                unsupported(exception);
            } catch (IOException exception) {
                ioError("Unable to load local map region", exception);
            } finally {
                requestedLoads.remove(key);
            }
        });
    }

    public Optional<MapRegion.Snapshot> snapshot(WaypointContext context, int regionX, int regionZ) {
        return Optional.ofNullable(published.get(new RegionKey(context, regionX, regionZ)));
    }

    /** Re-evaluates a raised quota without polling the filesystem concurrently. */
    public void recheckQuota() {
        if (closed.get() || status.state() != MapStoreStatus.State.QUOTA_REACHED
                || !quotaRecheckPending.compareAndSet(false, true)) {
            return;
        }
        submitWorker(() -> {
            try {
                diskBytes = countDiskBytes();
                if (diskBytes < byteLimit()) {
                    status = MapStoreStatus.ready();
                    scheduleFlush();
                }
            } catch (IOException exception) {
                ioError("Unable to recheck the local map storage limit", exception);
            } finally {
                quotaRecheckPending.set(false);
            }
        });
    }

    /** Queues a best-effort flush without blocking the caller. */
    public void flush() {
        if (!closed.get()) {
            submitWorker(this::flushAll);
        }
    }

    /** Test/diagnostic barrier that never runs on the render path. */
    boolean awaitIdle(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (worker.isTerminated()) {
            return true;
        }
        CountDownLatch barrier = new CountDownLatch(1);
        try {
            worker.execute(barrier::countDown);
            return barrier.await(Math.max(0L, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException exception) {
            return worker.isTerminated();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean closeAndFlush(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (!closed.compareAndSet(false, true)) {
            return worker.isTerminated();
        }
        status = new MapStoreStatus(MapStoreStatus.State.FLUSHING, "Saving local map");
        try {
            worker.execute(() -> {
                flushAll();
                status = new MapStoreStatus(MapStoreStatus.State.CLOSED, "Map storage closed");
            });
        } catch (RejectedExecutionException exception) {
            status = new MapStoreStatus(MapStoreStatus.State.CLOSED, "Map storage closed");
        }
        worker.shutdown();
        try {
            return worker.awaitTermination(Math.max(0L, timeout.toMillis()), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    @Override
    public void close() {
        closeAndFlush(Duration.ofSeconds(5));
    }

    private void switchContext(WaypointContext context) {
        flushAll();
        regions.clear();
        published.clear();
        publishedOrder.clear();
        requestedLoads.clear();
        missingRegions.clear();
        if (!context.equals(activeContext)) {
            return;
        }
        try {
            new MapStorageKey(mapsRoot, context).ensureMetadata();
            diskBytes = countDiskBytes();
            if (diskBytes >= byteLimit()) {
                quotaReached();
            } else {
                status = MapStoreStatus.ready();
            }
        } catch (MapStorageKey.UnsupportedMetadataVersionException exception) {
            unsupported(exception);
        } catch (IOException exception) {
            ioError("Unable to open local map context", exception);
        }
    }

    private void apply(MapChunkSnapshot snapshot) {
        int regionX = MapRegion.regionCoordinateForChunk(snapshot.chunkX());
        int regionZ = MapRegion.regionCoordinateForChunk(snapshot.chunkZ());
        RegionKey key = new RegionKey(snapshot.context(), regionX, regionZ);
        missingRegions.remove(key);
        try {
            CachedRegion cached = region(key);
            if (cached.region.apply(snapshot)) {
                cached.dirty = true;
                cached.changedChunks++;
                publish(key, cached.region.snapshot());
                if (cached.changedChunks >= FLUSH_CHANGE_THRESHOLD) {
                    flushRegion(key, cached);
                } else {
                    scheduleFlush();
                }
            }
        } catch (MapRegionCodec.UnsupportedVersionException
                 | MapStorageKey.UnsupportedMetadataVersionException exception) {
            unsupported(exception);
        } catch (IOException exception) {
            ioError("Unable to update local map region", exception);
        }
    }

    private CachedRegion region(RegionKey key) throws IOException {
        CachedRegion existing = regions.get(key);
        if (existing != null) {
            return existing;
        }
        MapStorageKey storage = new MapStorageKey(mapsRoot, key.context());
        storage.ensureMetadata();
        Path path = storage.regionPath(key.regionX(), key.regionZ());
        MapRegion.Snapshot loaded = codec.readWithRecovery(path, key.regionX(), key.regionZ());
        CachedRegion created = new CachedRegion(
                new MapRegion(key.regionX(), key.regionZ(), loaded.copyPixels(), loaded.revision()), path);
        regions.put(key, created);
        evictIfNeeded();
        return created;
    }

    private void evictIfNeeded() {
        while (regions.size() > MAXIMUM_CACHED_REGIONS) {
            Iterator<Map.Entry<RegionKey, CachedRegion>> iterator = regions.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Map.Entry<RegionKey, CachedRegion> eldest = iterator.next();
            if (eldest.getValue().dirty) {
                flushRegion(eldest.getKey(), eldest.getValue());
            }
            iterator.remove();
        }
    }

    private void scheduleFlush() {
        if (flushScheduled || closed.get()) {
            return;
        }
        flushScheduled = true;
        worker.schedule(() -> {
            flushScheduled = false;
            flushAll();
        }, FLUSH_DELAY_SECONDS, TimeUnit.SECONDS);
    }

    private void flushAll() {
        if (regions.isEmpty()) {
            return;
        }
        MapStoreStatus previous = status;
        if (previous.state() == MapStoreStatus.State.READY) {
            status = new MapStoreStatus(MapStoreStatus.State.FLUSHING, "Saving local map");
        }
        List<Map.Entry<RegionKey, CachedRegion>> entries = new ArrayList<>(regions.entrySet());
        for (Map.Entry<RegionKey, CachedRegion> entry : entries) {
            flushRegion(entry.getKey(), entry.getValue());
            if (status.state() == MapStoreStatus.State.QUOTA_REACHED
                    || status.state() == MapStoreStatus.State.IO_ERROR
                    || status.state() == MapStoreStatus.State.UNSUPPORTED_VERSION) {
                return;
            }
        }
        if (!closed.get() && activeContext != null) {
            status = MapStoreStatus.ready();
        }
    }

    private void flushRegion(RegionKey key, CachedRegion cached) {
        if (!cached.dirty) {
            return;
        }
        try {
            byte[] encoded = codec.encode(cached.region.snapshot());
            long projected = projectedDiskBytes(cached.path, encoded.length);
            if (projected > byteLimit()) {
                quotaReached();
                return;
            }
            codec.writeAtomic(cached.path, encoded);
            diskBytes = projected;
            cached.dirty = false;
            cached.changedChunks = 0;
        } catch (IOException exception) {
            ioError("Unable to save local map region", exception);
        }
    }

    private long projectedDiskBytes(Path primary, int encodedBytes) throws IOException {
        Path backup = MapRegionCodec.backupPath(primary);
        long primarySize = Files.exists(primary) ? Files.size(primary) : 0L;
        long backupSize = Files.exists(backup) ? Files.size(backup) : 0L;
        return Math.max(0L, diskBytes - primarySize - backupSize)
                + encodedBytes + (primarySize > 0L ? primarySize : 0L);
    }

    private long countDiskBytes() throws IOException {
        if (Files.notExists(mapsRoot)) {
            return 0L;
        }
        try (var paths = Files.walk(mapsRoot)) {
            return paths.filter(Files::isRegularFile).mapToLong(path -> {
                try {
                    return Files.size(path);
                } catch (IOException exception) {
                    throw new DiskCountException(exception);
                }
            }).sum();
        } catch (DiskCountException exception) {
            throw exception.cause;
        }
    }

    private long byteLimit() {
        int megabytes = Math.clamp(limitMegabytes.getAsInt(), 64, 4096);
        return megabytes * 1024L * 1024L;
    }

    private void publish(RegionKey key, MapRegion.Snapshot snapshot) {
        published.put(key, snapshot);
        publishedOrder.put(key, Boolean.TRUE);
        while (publishedOrder.size() > MAXIMUM_PUBLISHED_REGIONS) {
            Iterator<RegionKey> iterator = publishedOrder.keySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            RegionKey eldest = iterator.next();
            iterator.remove();
            published.remove(eldest);
        }
    }

    private void quotaReached() {
        status = new MapStoreStatus(MapStoreStatus.State.QUOTA_REACHED,
                "Map storage limit reached; recording is paused");
    }

    private void unsupported(Exception exception) {
        status = new MapStoreStatus(MapStoreStatus.State.UNSUPPORTED_VERSION,
                "Saved map data was created by a newer client");
        LOGGER.log(Level.WARNING, "Unable to open newer map data without changing it", exception);
    }

    private void ioError(String message, IOException exception) {
        status = new MapStoreStatus(MapStoreStatus.State.IO_ERROR,
                "Local map storage is unavailable; recording is paused");
        LOGGER.log(Level.WARNING, message, exception);
    }

    private void submitWorker(Runnable task) {
        try {
            worker.execute(task);
        } catch (RejectedExecutionException exception) {
            if (!closed.get()) {
                LOGGER.log(Level.WARNING, "Persistent map worker rejected a task", exception);
            }
        }
    }

    private record RegionKey(WaypointContext context, int regionX, int regionZ) {
        private RegionKey {
            context = Objects.requireNonNull(context, "context");
        }
    }

    private static final class CachedRegion {
        private final MapRegion region;
        private final Path path;
        private boolean dirty;
        private int changedChunks;

        private CachedRegion(MapRegion region, Path path) {
            this.region = region;
            this.path = path;
        }
    }

    private static final class DiskCountException extends RuntimeException {
        private final IOException cause;

        private DiskCountException(IOException cause) {
            this.cause = cause;
        }
    }
}
