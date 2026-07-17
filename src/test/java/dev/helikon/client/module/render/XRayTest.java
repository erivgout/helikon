package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XRayTest {
    @AfterEach
    void restoreSharedRenderState() {
        XRayRenderAccess.deactivate();
        XRayRenderAccess.endChunkCompilation();
    }

    @Test
    void rebuildsLocalGeometryForEnableSettingChangesAndDisable() {
        RecordingInvalidator invalidator = new RecordingInvalidator();
        XRay xray = new XRay(invalidator);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(xray);

        registry.setEnabled(xray, true);
        assertTrue(XRayRenderAccess.state().active());
        assertTrue(XRayRenderAccess.hides("minecraft:stone"));
        assertEquals(1, invalidator.calls);

        xray.blocks().set("minecraft:stone");
        assertFalse(XRayRenderAccess.hides("minecraft:stone"));
        assertEquals(2, invalidator.calls);

        xray.opacity().set(0.4D);
        assertEquals(0.4F, XRayRenderAccess.opacity());
        assertEquals(3, invalidator.calls);

        registry.setEnabled(xray, false);
        assertFalse(XRayRenderAccess.state().active());
        assertFalse(XRayRenderAccess.hides("minecraft:diamond_ore"));
        assertEquals(4, invalidator.calls);
    }

    private static final class RecordingInvalidator implements XRay.RendererInvalidator {
        private int calls;

        @Override
        public void invalidateGeometry() {
            calls++;
        }
    }
}
