package dev.helikon.client.module;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Optional local timing recorder with no work on normal disabled-module paths. */
public final class ModuleTimingMetrics {
    private final Map<String, MutableTiming> timings = new LinkedHashMap<>();
    private volatile boolean recording;

    public boolean isRecording() {
        return recording;
    }

    /** Starts a fresh local measurement session or stops collecting immediately. */
    public synchronized void setRecording(boolean shouldRecord) {
        if (recording == shouldRecord) {
            return;
        }
        recording = shouldRecord;
        if (shouldRecord) {
            timings.clear();
        }
    }

    /** Records one already-measured module operation when the debug overlay is enabled. */
    public void record(String moduleId, String operation, long durationNanos) {
        if (!recording) {
            return;
        }
        if (durationNanos < 0L) {
            throw new IllegalArgumentException("durationNanos must not be negative");
        }
        Kind kind = kindFor(operation);
        if (kind == null) {
            return;
        }

        synchronized (this) {
            if (!recording) {
                return;
            }
            MutableTiming timing = timings.computeIfAbsent(requireId(moduleId), ignored -> new MutableTiming());
            if (kind == Kind.TICK) {
                timing.tickNanos = durationNanos;
                timing.tickSamples++;
            } else {
                timing.renderNanos = durationNanos;
                timing.renderSamples++;
            }
        }
    }

    /** Returns one stable timing row for every registered module, including modules with no measured work. */
    public synchronized List<Snapshot> snapshots(Collection<Module> modules) {
        Objects.requireNonNull(modules, "modules");
        List<Snapshot> result = new ArrayList<>(modules.size());
        for (Module module : modules) {
            Module nonNullModule = Objects.requireNonNull(module, "module");
            MutableTiming timing = timings.get(nonNullModule.id());
            result.add(timing == null
                    ? new Snapshot(nonNullModule.id(), 0L, 0L, 0L, 0L)
                    : new Snapshot(nonNullModule.id(), timing.tickNanos, timing.renderNanos,
                    timing.tickSamples, timing.renderSamples));
        }
        return List.copyOf(result);
    }

    private static Kind kindFor(String operation) {
        String checkedOperation = Objects.requireNonNull(operation, "operation");
        return switch (checkedOperation) {
            case "tick", "scan" -> Kind.TICK;
            case "render" -> Kind.RENDER;
            default -> null;
        };
    }

    private static String requireId(String moduleId) {
        String checkedId = Objects.requireNonNull(moduleId, "moduleId");
        if (checkedId.isBlank()) {
            throw new IllegalArgumentException("moduleId must not be blank");
        }
        return checkedId;
    }

    /** Immutable local timing snapshot in nanoseconds. */
    public record Snapshot(String moduleId, long tickNanos, long renderNanos, long tickSamples, long renderSamples) {
        public Snapshot {
            requireId(moduleId);
            if (tickNanos < 0L || renderNanos < 0L || tickSamples < 0L || renderSamples < 0L) {
                throw new IllegalArgumentException("Timing values must not be negative");
            }
        }
    }

    private enum Kind {
        TICK,
        RENDER
    }

    private static final class MutableTiming {
        private long tickNanos;
        private long renderNanos;
        private long tickSamples;
        private long renderSamples;
    }
}
