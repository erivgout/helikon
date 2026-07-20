package dev.helikon.client.event;

import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonInfo;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/** Narrow client-only bridge used by verified Minecraft input and world lifecycle mixins. */
public final class ClientEventAccess {
    private static final Object RESOURCE_RELOAD_LOCK = new Object();
    private static final Set<CompletableFuture<Void>> ACTIVE_RESOURCE_RELOADS =
            Collections.newSetFromMap(new IdentityHashMap<>());
    private static volatile EventBus events;

    private ClientEventAccess() {
    }

    public static void install(EventBus eventBus) {
        events = Objects.requireNonNull(eventBus, "eventBus");
        synchronized (RESOURCE_RELOAD_LOCK) {
            ACTIVE_RESOURCE_RELOADS.clear();
        }
    }

    public static void postKey(int action, KeyEvent event) {
        InputEvent.Action mapped = action(action);
        if (mapped != null) {
            post(new InputEvent(InputEvent.Kind.KEY, mapped, event.key(), event.scancode(), event.modifiers(), 0.0D, 0.0D));
        }
    }

    public static void postMouseButton(int action, MouseButtonInfo event) {
        InputEvent.Action mapped = action(action);
        if (mapped != null) {
            post(new InputEvent(InputEvent.Kind.MOUSE_BUTTON, mapped, event.button(), 0, event.modifiers(), 0.0D, 0.0D));
        }
    }

    public static void postMouseScroll(double scrollX, double scrollY) {
        if (Double.isFinite(scrollX) && Double.isFinite(scrollY)) {
            post(new InputEvent(InputEvent.Kind.MOUSE_SCROLL, InputEvent.Action.SCROLL, 0, 0, 0, scrollX, scrollY));
        }
    }

    public static void postChunk(ChunkEvent.Phase phase, int chunkX, int chunkZ) {
        post(new ChunkEvent(phase, chunkX, chunkZ));
    }

    /** Publishes one block state after Minecraft has accepted it into the current client world. */
    public static void postBlockChange(int x, int y, int z, String blockId) {
        post(new BlockChangeEvent(x, y, z, blockId));
    }

    /** Publishes a render boundary using only its local primitive metadata. */
    public static void postRender(RenderEvent.Kind kind, double tickProgress, String subjectId) {
        post(new RenderEvent(kind, tickProgress, subjectId));
    }

    /** Publishes an ordinary local interaction after Minecraft has accepted it. */
    public static void postInteraction(InteractionEvent.Kind kind, String subjectId) {
        post(new InteractionEvent(kind, subjectId));
    }

    /** Publishes a packet boundary without retaining or exposing the packet object. */
    public static void postPacket(PacketObservationEvent.Direction direction, String packetType) {
        post(new PacketObservationEvent(direction, packetType));
    }

    /** Returns whether packet processing changed an absent cache entry into a loaded chunk. */
    public static boolean isInitialChunkLoad(boolean wasLoaded, boolean packetReturnedChunk) {
        return !wasLoaded && packetReturnedChunk;
    }

    /** Returns whether dropping a previously loaded chunk actually removed its cache entry. */
    public static boolean isActualChunkUnload(boolean wasLoaded, boolean isLoadedAfterDrop) {
        return wasLoaded && !isLoadedAfterDrop;
    }

    public static void postResourceReload(ResourceReloadEvent.Phase phase) {
        post(new ResourceReloadEvent(Objects.requireNonNull(phase, "phase")));
    }

    /**
     * Registers a distinct resource reload future and posts its start exactly once.
     * Minecraft returns the same pending future for overlapping reload requests.
     */
    public static boolean beginResourceReload(CompletableFuture<Void> reload) {
        Objects.requireNonNull(reload, "reload");
        synchronized (RESOURCE_RELOAD_LOCK) {
            if (!ACTIVE_RESOURCE_RELOADS.add(reload)) {
                return false;
            }
        }
        postResourceReload(ResourceReloadEvent.Phase.START);
        return true;
    }

    /** Finishes a tracked reload, posting COMPLETE only for a successful reload. */
    public static void finishResourceReload(CompletableFuture<Void> reload, boolean succeeded) {
        Objects.requireNonNull(reload, "reload");
        boolean observed;
        synchronized (RESOURCE_RELOAD_LOCK) {
            observed = ACTIVE_RESOURCE_RELOADS.remove(reload);
        }
        if (observed && succeeded) {
            postResourceReload(ResourceReloadEvent.Phase.COMPLETE);
        }
    }

    private static InputEvent.Action action(int action) {
        return switch (action) {
            case GLFW.GLFW_PRESS -> InputEvent.Action.PRESS;
            case GLFW.GLFW_RELEASE -> InputEvent.Action.RELEASE;
            case GLFW.GLFW_REPEAT -> InputEvent.Action.REPEAT;
            default -> null;
        };
    }

    private static void post(ClientEvent event) {
        EventBus current = events;
        if (current != null) {
            current.post(event);
        }
    }
}
