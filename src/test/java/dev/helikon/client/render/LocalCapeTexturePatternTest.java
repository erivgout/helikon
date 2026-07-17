package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LocalCapeTexturePatternTest {
    @Test
    void producesAnOpaquePrimaryFieldWithTheCenteredAccentEmblemAndDarkBorder() {
        int primary = 0x20556677;
        int accent = 0x4088AACC;

        assertEquals(0xFF556677, LocalCapeTexturePattern.argbAt(12, 12, primary, accent));
        assertEquals(0xFF88AACC, LocalCapeTexturePattern.argbAt(28, 15, primary, accent));
        assertEquals(0xFF3F4C59, LocalCapeTexturePattern.argbAt(0, 12, primary, accent));
    }

    @Test
    void convertsArgbToNativeImagesAbgrOrdering() {
        assertEquals(0x11443322, LocalCapeTexturePattern.toAbgr(0x11223344));
    }

    @Test
    void rejectsPixelsOutsideTheFixedCapeTexture() {
        assertThrows(IllegalArgumentException.class, () -> LocalCapeTexturePattern.argbAt(-1, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> LocalCapeTexturePattern.argbAt(64, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> LocalCapeTexturePattern.argbAt(0, 32, 0, 0));
    }
}
