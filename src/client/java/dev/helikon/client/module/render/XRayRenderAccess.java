package dev.helikon.client.module.render;

import dev.helikon.client.render.XRayRenderState;

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/** Thread-safe module snapshot bridge for the verified local chunk compiler mixins. */
public final class XRayRenderAccess {
    private static final AtomicReference<XRayRenderState> STATE = new AtomicReference<>(XRayRenderState.disabled());
    private static final ThreadLocal<Boolean> CHUNK_COMPILATION = ThreadLocal.withInitial(() -> false);

    private XRayRenderAccess() {
    }

    static void activate(Set<String> targets, float opacity) {
        STATE.set(targets.isEmpty() ? XRayRenderState.disabled() : new XRayRenderState(true, targets, opacity));
    }

    static void deactivate() {
        STATE.set(XRayRenderState.disabled());
    }

    public static boolean hides(String blockId) {
        return STATE.get().hides(blockId);
    }

    public static boolean isCompilingXRayChunk() {
        return CHUNK_COMPILATION.get() && STATE.get().active();
    }

    public static float opacity() {
        return STATE.get().opacity();
    }

    public static void beginChunkCompilation() {
        CHUNK_COMPILATION.set(true);
    }

    public static void endChunkCompilation() {
        CHUNK_COMPILATION.remove();
    }

    static XRayRenderState state() {
        return STATE.get();
    }
}
