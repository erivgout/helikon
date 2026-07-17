package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NewChunksTest {
    @Test
    void classifiesFirstLoadsAndLaterReloadsOnlyWhileEnabled() {
        NewChunks chunks = new NewChunks();

        assertEquals("new_chunks", chunks.id());
        assertEquals("NewChunks", chunks.name());
        assertEquals(ModuleCategory.RENDER, chunks.category());
        assertFalse(chunks.defaultEnabled());
        chunks.observe(NewChunks.ChunkPhase.LOAD, 3, -2, 100L);
        assertTrue(chunks.visibleMarkers(3, -2, 100L).isEmpty());

        chunks.enable();
        chunks.observe(NewChunks.ChunkPhase.LOAD, 3, -2, 100L);
        assertEquals(NewChunks.LoadKind.FIRST_SEEN,
                chunks.visibleMarkers(3, -2, 100L).getFirst().kind());
        chunks.observe(NewChunks.ChunkPhase.UNLOAD, 3, -2, 200L);
        assertTrue(chunks.visibleMarkers(3, -2, 200L).isEmpty());
        chunks.observe(NewChunks.ChunkPhase.LOAD, 3, -2, 300L);
        assertEquals(NewChunks.LoadKind.RELOADED,
                chunks.visibleMarkers(3, -2, 300L).getFirst().kind());
    }

    @Test
    void expiresAndRangeCullsBoundedSessionMarkers() {
        NewChunks chunks = new NewChunks();
        chunks.enable();
        integerSetting(chunks, "lifetime_seconds").set(5);
        chunks.observe(NewChunks.ChunkPhase.LOAD, 0, 0, 0L);
        chunks.observe(NewChunks.ChunkPhase.LOAD, 20, 0, 0L);

        assertEquals(List.of(new NewChunks.Marker(
                new NewChunks.ChunkCoordinate(0, 0), NewChunks.LoadKind.FIRST_SEEN, 0L)),
                chunks.visibleMarkers(0, 0, 5_000L));
        assertTrue(chunks.visibleMarkers(0, 0, 5_001L).isEmpty());
    }

    @Test
    void disableClearsAllTransientChunkHistoryAndSettingsStayBounded() {
        NewChunks chunks = new NewChunks();
        IntegerSetting maximum = integerSetting(chunks, "maximum_chunks");
        IntegerSetting lifetime = integerSetting(chunks, "lifetime_seconds");

        assertEquals(16, maximum.minimum());
        assertEquals(1_024, maximum.maximum());
        assertEquals(5, lifetime.minimum());
        assertEquals(600, lifetime.maximum());
        assertThrows(IllegalArgumentException.class, () -> maximum.set(1_025));

        chunks.enable();
        chunks.observe(NewChunks.ChunkPhase.LOAD, 0, 0, 1L);
        chunks.disable();
        chunks.enable();
        chunks.observe(NewChunks.ChunkPhase.LOAD, 0, 0, 2L);
        assertEquals(NewChunks.LoadKind.FIRST_SEEN,
                chunks.visibleMarkers(0, 0, 2L).getFirst().kind());
    }

    private static IntegerSetting integerSetting(NewChunks module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
