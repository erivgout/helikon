package dev.helikon.client.event;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientEventAccessTest {
    @Test
    void normalizesVerifiedInputAndLifecycleObservations() {
        List<ClientEvent> observed = new ArrayList<>();
        EventBus events = new EventBus((event, exception) -> { throw exception; });
        events.subscribe(InputEvent.class, observed::add);
        events.subscribe(ChunkEvent.class, observed::add);
        events.subscribe(ResourceReloadEvent.class, observed::add);
        ClientEventAccess.install(events);

        ClientEventAccess.postKey(GLFW.GLFW_PRESS, new KeyEvent(GLFW.GLFW_KEY_R, 17, GLFW.GLFW_MOD_CONTROL));
        ClientEventAccess.postMouseButton(GLFW.GLFW_RELEASE,
                new MouseButtonInfo(GLFW.GLFW_MOUSE_BUTTON_4, GLFW.GLFW_MOD_SHIFT));
        ClientEventAccess.postMouseScroll(1.25D, -2.5D);
        ClientEventAccess.postChunk(ChunkEvent.Phase.LOAD, 4, -8);
        ClientEventAccess.postResourceReload(ResourceReloadEvent.Phase.COMPLETE);

        assertEquals(new InputEvent(InputEvent.Kind.KEY, InputEvent.Action.PRESS, GLFW.GLFW_KEY_R, 17,
                GLFW.GLFW_MOD_CONTROL, 0.0D, 0.0D), observed.get(0));
        assertEquals(new InputEvent(InputEvent.Kind.MOUSE_BUTTON, InputEvent.Action.RELEASE, GLFW.GLFW_MOUSE_BUTTON_4,
                0, GLFW.GLFW_MOD_SHIFT, 0.0D, 0.0D), observed.get(1));
        assertEquals(new InputEvent(InputEvent.Kind.MOUSE_SCROLL, InputEvent.Action.SCROLL, 0, 0, 0,
                1.25D, -2.5D), observed.get(2));
        assertEquals(new ChunkEvent(ChunkEvent.Phase.LOAD, 4, -8), observed.get(3));
        assertEquals(new ResourceReloadEvent(ResourceReloadEvent.Phase.COMPLETE), observed.get(4));
    }

    @Test
    void reportsOnlyActualChunkCacheTransitions() {
        assertTrue(ClientEventAccess.isInitialChunkLoad(false, true));
        assertFalse(ClientEventAccess.isInitialChunkLoad(true, true));
        assertFalse(ClientEventAccess.isInitialChunkLoad(false, false));

        assertTrue(ClientEventAccess.isActualChunkUnload(true, false));
        assertFalse(ClientEventAccess.isActualChunkUnload(false, false));
        assertFalse(ClientEventAccess.isActualChunkUnload(true, true));
    }

    @Test
    void observesEachResourceReloadFutureOnce() {
        List<ClientEvent> observed = new ArrayList<>();
        EventBus events = new EventBus((event, exception) -> { throw exception; });
        events.subscribe(ResourceReloadEvent.class, observed::add);
        ClientEventAccess.install(events);

        CompletableFuture<Void> firstReload = new CompletableFuture<>();
        assertTrue(ClientEventAccess.beginResourceReload(firstReload));
        assertFalse(ClientEventAccess.beginResourceReload(firstReload));
        ClientEventAccess.finishResourceReload(firstReload, true);

        CompletableFuture<Void> failedReload = new CompletableFuture<>();
        assertTrue(ClientEventAccess.beginResourceReload(failedReload));
        ClientEventAccess.finishResourceReload(failedReload, false);

        assertEquals(List.of(
                new ResourceReloadEvent(ResourceReloadEvent.Phase.START),
                new ResourceReloadEvent(ResourceReloadEvent.Phase.COMPLETE),
                new ResourceReloadEvent(ResourceReloadEvent.Phase.START)), observed);
    }
}
