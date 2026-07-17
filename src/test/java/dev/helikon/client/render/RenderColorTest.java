package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RenderColorTest {
    @Test
    void multipliesArgbAlphaWithoutChangingRgbChannels() {
        assertEquals(0x404488CC, RenderColor.withAlpha(0x804488CC, 0.5D));
        assertEquals(0x004488CC, RenderColor.withAlpha(0x804488CC, -2.0D));
        assertEquals(0xFF4488CC, RenderColor.withAlpha(0x804488CC, 2.0D));
    }

    @Test
    void rejectsNonFiniteTransparency() {
        assertThrows(IllegalArgumentException.class, () -> RenderColor.withAlpha(0xFFFFFFFF, Double.NaN));
    }
}
