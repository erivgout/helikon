package dev.helikon.client.map;

import dev.helikon.client.render.RadarMapColor;
import dev.helikon.client.waypoint.WaypointContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

import java.util.Optional;

/** Client-thread-only Minecraft 26.2 adapter for one loaded terrain chunk. */
public final class MinecraftMapChunkSampler {
    private static final int CEILING_SCAN_DEPTH = 64;

    public Optional<MapChunkSnapshot> sample(WaypointContext context, int chunkX, int chunkZ) {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        if (level == null || client.player == null) {
            return Optional.empty();
        }
        LevelChunk chunk = level.getChunkSource().getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
        if (chunk == null) {
            return Optional.empty();
        }
        int[] pixels = new int[MapChunkSnapshot.PIXEL_COUNT];
        boolean ceiling = level.dimensionType().hasCeiling();
        int playerY = client.player.getBlockY();
        int startX = chunkX * MapChunkSnapshot.SIZE;
        int startZ = chunkZ * MapChunkSnapshot.SIZE;
        for (int localZ = 0; localZ < MapChunkSnapshot.SIZE; localZ++) {
            int previousHeight = Integer.MIN_VALUE;
            for (int localX = 0; localX < MapChunkSnapshot.SIZE; localX++) {
                int worldX = startX + localX;
                int worldZ = startZ + localZ;
                Sample sample = ceiling
                        ? ceilingSample(level, worldX, worldZ, playerY)
                        : surfaceSample(level, worldX, worldZ);
                if (sample == null) {
                    continue;
                }
                int slope = previousHeight == Integer.MIN_VALUE ? 0 : sample.height() - previousHeight;
                pixels[localZ * MapChunkSnapshot.SIZE + localX] =
                        RadarMapColor.forMapColor(sample.color(), 0, slope);
                previousHeight = sample.height();
            }
        }
        return Optional.of(new MapChunkSnapshot(context, chunkX, chunkZ, pixels));
    }

    private static Sample surfaceSample(ClientLevel level, int worldX, int worldZ) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ) - 1;
        if (surfaceY < level.getMinY()) {
            return null;
        }
        BlockPos position = new BlockPos(worldX, surfaceY, worldZ);
        if (!level.isLoaded(position)) {
            return null;
        }
        return sample(level, position);
    }

    private static Sample ceilingSample(ClientLevel level, int worldX, int worldZ, int playerY) {
        int maximumY = Math.min(level.getMaxY() - 1, playerY + 1);
        int minimumY = Math.max(level.getMinY(), maximumY - CEILING_SCAN_DEPTH);
        BlockPos.MutableBlockPos position = new BlockPos.MutableBlockPos(worldX, maximumY, worldZ);
        for (int y = maximumY; y >= minimumY; y--) {
            position.setY(y);
            BlockState state = level.getBlockState(position);
            if (state.isAir()) {
                continue;
            }
            if (!state.getFluidState().isEmpty() || !state.getCollisionShape(level, position).isEmpty()) {
                return sample(level, position);
            }
        }
        return null;
    }

    private static Sample sample(ClientLevel level, BlockPos position) {
        BlockState state = level.getBlockState(position);
        MapColor mapColor = state.getMapColor(level, position);
        int color;
        if (mapColor == MapColor.NONE) {
            String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
            color = RadarMapColor.forBlock(blockId, 0);
        } else {
            color = mapColor.calculateARGBColor(MapColor.Brightness.NORMAL);
        }
        return new Sample(position.getY(), color | 0xFF000000);
    }

    private record Sample(int height, int color) {
    }
}
