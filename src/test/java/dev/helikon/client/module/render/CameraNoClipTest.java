package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CameraNoClipTest {
    @Test
    void onlyRequestsUnclippedDistanceWhileEnabled() {
        CameraNoClip module = new CameraNoClip();

        assertEquals("camera_no_clip", module.id());
        assertFalse(module.defaultEnabled());
        assertFalse(module.usesUnclippedCameraDistance());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        assertTrue(module.usesUnclippedCameraDistance());

        registry.setEnabled(module, false);
        assertFalse(module.usesUnclippedCameraDistance());
    }
}
