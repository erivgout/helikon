package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.ColorSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Tracks bounded local chunk-cache load observations for session-only rendering. */
public final class NewChunks extends Module {
    private static final int MAXIMUM_SEEN_CHUNKS = 4_096;

    public enum LoadKind {
        FIRST_SEEN,
        RELOADED
    }

    public enum ChunkPhase {
        LOAD,
        UNLOAD
    }

    private final BooleanSetting markFirstSeen;
    private final BooleanSetting markReloads;
    private final BooleanSetting retainUnloaded;
    private final IntegerSetting lifetimeSeconds;
    private final IntegerSetting maximumChunks;
    private final IntegerSetting rangeChunks;
    private final IntegerSetting verticalSpan;
    private final BooleanSetting alwaysOnTop;
    private final NumberSetting lineWidth;
    private final NumberSetting fillOpacity;
    private final ColorSetting firstSeenColor;
    private final ColorSetting reloadColor;
    private final LinkedHashMap<ChunkCoordinate, Marker> markers = new LinkedHashMap<>();
    private final LinkedHashSet<ChunkCoordinate> seenChunks = new LinkedHashSet<>();
    private final Set<ChunkCoordinate> loadedChunks = new HashSet<>();

    public NewChunks() {
        super("new_chunks", "NewChunks", "Marks chunks first seen or reloaded by the local chunk cache.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        markFirstSeen = addSetting(new BooleanSetting("mark_first_seen", "Mark first seen",
                "Mark the first load observed for a coordinate while the module is enabled.", true));
        markReloads = addSetting(new BooleanSetting("mark_reloads", "Mark reloads",
                "Mark coordinates loaded again after an observed unload.", true));
        retainUnloaded = addSetting(new BooleanSetting("retain_unloaded", "Retain unloaded",
                "Keep a session marker after its chunk leaves the local cache.", false));
        lifetimeSeconds = addSetting(new IntegerSetting("lifetime_seconds", "Marker lifetime",
                "Seconds before a local load marker expires.", 120, 5, 600));
        maximumChunks = addSetting(new IntegerSetting("maximum_chunks", "Maximum chunks",
                "Hard cap for retained local chunk markers.", 256, 16, 1_024));
        rangeChunks = addSetting(new IntegerSetting("range_chunks", "Range",
                "Maximum horizontal marker distance in chunks.", 12, 1, 32));
        verticalSpan = addSetting(new IntegerSetting("vertical_span", "Vertical span",
                "Height of each local chunk-boundary prism around the player.", 128, 16, 384));
        alwaysOnTop = addSetting(new BooleanSetting("always_on_top", "Always on top",
                "Draw local chunk boundaries through terrain.", true));
        lineWidth = addSetting(new NumberSetting("line_width", "Line width",
                "Local chunk-boundary line width.", 1.0D, 0.5D, 4.0D));
        fillOpacity = addSetting(new NumberSetting("fill_opacity", "Fill opacity",
                "Local chunk-prism fill opacity.", 0.08D, 0.0D, 0.5D));
        firstSeenColor = addSetting(new ColorSetting("first_seen_color", "First-seen color",
                "ARGB outline color for coordinates first observed while enabled.", 0xFF66BB6A));
        reloadColor = addSetting(new ColorSetting("reload_color", "Reload color",
                "ARGB outline color for coordinates observed loading again.", 0xFF42A5F5));
        maximumChunks.addChangeListener(ignored -> trimMarkers());
        retainUnloaded.addChangeListener(retain -> {
            if (!retain) {
                markers.keySet().removeIf(coordinate -> !loadedChunks.contains(coordinate));
            }
        });
    }

    /** Applies one already-validated local chunk-cache transition. */
    public void observe(ChunkPhase phase, int chunkX, int chunkZ, long nowMillis) {
        if (!isEnabled() || phase == null || nowMillis < 0L) {
            return;
        }
        ChunkCoordinate coordinate = new ChunkCoordinate(chunkX, chunkZ);
        if (phase == ChunkPhase.UNLOAD) {
            loadedChunks.remove(coordinate);
            if (!retainUnloaded.value()) {
                markers.remove(coordinate);
            }
            return;
        }
        if (!loadedChunks.add(coordinate)) {
            return;
        }
        boolean firstSeen = seenChunks.add(coordinate);
        trimSeenChunks();
        LoadKind kind = firstSeen ? LoadKind.FIRST_SEEN : LoadKind.RELOADED;
        if ((kind == LoadKind.FIRST_SEEN && !markFirstSeen.value())
                || (kind == LoadKind.RELOADED && !markReloads.value())) {
            return;
        }
        markers.remove(coordinate);
        markers.put(coordinate, new Marker(coordinate, kind, nowMillis));
        trimMarkers();
    }

    /** Returns an immutable current-range snapshot after expiring old entries. */
    public List<Marker> visibleMarkers(int playerChunkX, int playerChunkZ, long nowMillis) {
        if (!isEnabled() || nowMillis < 0L) {
            return List.of();
        }
        expire(nowMillis);
        List<Marker> visible = new ArrayList<>();
        int range = rangeChunks.value();
        for (Marker marker : markers.values()) {
            long deltaX = (long) marker.coordinate().x() - playerChunkX;
            long deltaZ = (long) marker.coordinate().z() - playerChunkZ;
            if (Math.abs(deltaX) <= range && Math.abs(deltaZ) <= range) {
                visible.add(marker);
            }
        }
        return List.copyOf(visible);
    }

    public int verticalSpan() {
        return verticalSpan.value();
    }

    public boolean alwaysOnTop() {
        return alwaysOnTop.value();
    }

    public float lineWidth() {
        return (float) lineWidth.value().doubleValue();
    }

    public int outlineColor(LoadKind kind) {
        return kind == LoadKind.RELOADED ? reloadColor.value() : firstSeenColor.value();
    }

    public int fillColor(LoadKind kind) {
        int color = outlineColor(kind);
        int alpha = (int) Math.round(fillOpacity.value() * 255.0D);
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    public void clear() {
        markers.clear();
        seenChunks.clear();
        loadedChunks.clear();
    }

    @Override
    protected void onDisable() {
        clear();
    }

    private void expire(long nowMillis) {
        long maximumAgeMillis = lifetimeSeconds.value() * 1_000L;
        Iterator<Map.Entry<ChunkCoordinate, Marker>> iterator = markers.entrySet().iterator();
        while (iterator.hasNext()) {
            Marker marker = iterator.next().getValue();
            if (nowMillis - marker.loadedAtMillis() > maximumAgeMillis) {
                iterator.remove();
            }
        }
    }

    private void trimMarkers() {
        while (markers.size() > maximumChunks.value()) {
            markers.remove(markers.keySet().iterator().next());
        }
    }

    private void trimSeenChunks() {
        while (seenChunks.size() > MAXIMUM_SEEN_CHUNKS) {
            seenChunks.remove(seenChunks.iterator().next());
        }
    }

    public record ChunkCoordinate(int x, int z) {
    }

    public record Marker(ChunkCoordinate coordinate, LoadKind kind, long loadedAtMillis) {
        public Marker {
            if (coordinate == null || kind == null || loadedAtMillis < 0L) {
                throw new IllegalArgumentException("Marker fields must be valid");
            }
        }
    }
}
