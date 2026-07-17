package dev.helikon.client.mixin;

import dev.helikon.client.event.ChunkEvent;
import dev.helikon.client.event.ClientEventAccess;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;

/** Observes only the local loaded-chunk cache lifecycle. */
@Mixin(ClientChunkCache.class)
abstract class ClientChunkCacheEventMixin {
    @Unique
    private final ThreadLocal<Deque<Boolean>> helikon$chunkWasLoaded =
            ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "drop", at = @At("HEAD"))
    private void helikon$rememberChunkBeforeUnload(ChunkPos position, CallbackInfo callback) {
        helikon$chunkWasLoaded.get().push(helikon$contains(position.x(), position.z()));
    }

    @Inject(method = "drop", at = @At("TAIL"))
    private void helikon$postChunkUnload(ChunkPos position, CallbackInfo callback) {
        boolean wasLoaded = helikon$chunkWasLoaded.get().pop();
        if (ClientEventAccess.isActualChunkUnload(wasLoaded, helikon$contains(position.x(), position.z()))) {
            ClientEventAccess.postChunk(ChunkEvent.Phase.UNLOAD, position.x(), position.z());
        }
    }

    @Inject(method = "replaceWithPacketData", at = @At("HEAD"))
    private void helikon$rememberChunkBeforeLoad(int chunkX, int chunkZ, FriendlyByteBuf buffer,
                                                  Map<Heightmap.Types, long[]> heightmaps,
                                                  Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                                  CallbackInfoReturnable<?> callback) {
        helikon$chunkWasLoaded.get().push(helikon$contains(chunkX, chunkZ));
    }

    @Inject(method = "replaceWithPacketData", at = @At("TAIL"))
    private void helikon$postChunkLoad(int chunkX, int chunkZ, FriendlyByteBuf buffer,
                                       Map<Heightmap.Types, long[]> heightmaps,
                                       Consumer<ClientboundLevelChunkPacketData.BlockEntityTagOutput> consumer,
                                       CallbackInfoReturnable<?> callback) {
        boolean wasLoaded = helikon$chunkWasLoaded.get().pop();
        if (ClientEventAccess.isInitialChunkLoad(wasLoaded, callback.getReturnValue() != null)) {
            ClientEventAccess.postChunk(ChunkEvent.Phase.LOAD, chunkX, chunkZ);
        }
    }

    @Unique
    private boolean helikon$contains(int chunkX, int chunkZ) {
        return ((ClientChunkCache) (Object) this).getChunk(chunkX, chunkZ, ChunkStatus.FULL, false) != null;
    }
}
