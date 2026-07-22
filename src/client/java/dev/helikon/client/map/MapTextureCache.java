package dev.helikon.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import dev.helikon.client.waypoint.WaypointContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/** Client-thread LRU for full-screen map region GPU textures. */
public final class MapTextureCache implements AutoCloseable {
    public static final int MAXIMUM_TEXTURES = 64;
    private static final AtomicLong NEXT_TEXTURE_ID = new AtomicLong();

    private final Minecraft client;
    private final Map<Key, Entry> textures = new LinkedHashMap<>(16, 0.75F, true);

    public MapTextureCache(Minecraft client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public Identifier texture(WaypointContext context, MapRegion.Snapshot snapshot) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(snapshot, "snapshot");
        Key key = new Key(context, snapshot.regionX(), snapshot.regionZ());
        Entry entry = textures.get(key);
        if (entry == null) {
            Identifier identifier = Identifier.fromNamespaceAndPath("helikon",
                    "map/region_" + NEXT_TEXTURE_ID.incrementAndGet());
            DynamicTexture texture = new DynamicTexture("Helikon discovered map region",
                    MapRegion.SIZE, MapRegion.SIZE, true);
            client.getTextureManager().register(identifier, texture);
            entry = new Entry(identifier, texture, Long.MIN_VALUE);
            textures.put(key, entry);
        }
        if (entry.revision != snapshot.revision()) {
            upload(entry.texture, snapshot);
            entry.revision = snapshot.revision();
        }
        evictIfNeeded();
        return entry.identifier;
    }

    public int size() {
        return textures.size();
    }

    public void clear() {
        for (Entry entry : textures.values()) {
            client.getTextureManager().release(entry.identifier);
        }
        textures.clear();
    }

    @Override
    public void close() {
        clear();
    }

    private static void upload(DynamicTexture texture, MapRegion.Snapshot snapshot) {
        NativeImage pixels = Objects.requireNonNull(texture.getPixels(), "map texture pixels");
        int[] colors = snapshot.copyPixels();
        for (int z = 0; z < MapRegion.SIZE; z++) {
            for (int x = 0; x < MapRegion.SIZE; x++) {
                pixels.setPixel(x, z, colors[z * MapRegion.SIZE + x]);
            }
        }
        texture.upload();
    }

    private void evictIfNeeded() {
        while (textures.size() > MAXIMUM_TEXTURES) {
            Iterator<Map.Entry<Key, Entry>> iterator = textures.entrySet().iterator();
            if (!iterator.hasNext()) {
                return;
            }
            Entry entry = iterator.next().getValue();
            iterator.remove();
            client.getTextureManager().release(entry.identifier);
        }
    }

    private record Key(WaypointContext context, int regionX, int regionZ) {
    }

    private static final class Entry {
        private final Identifier identifier;
        private final DynamicTexture texture;
        private long revision;

        private Entry(Identifier identifier, DynamicTexture texture, long revision) {
            this.identifier = identifier;
            this.texture = texture;
            this.revision = revision;
        }
    }
}

