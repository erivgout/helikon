package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChamsColorPolicyTest {
    private static final int BASE = 0xFFFF4D4D;
    private static final int FRIEND = 0xFF61D17B;

    @Test
    void friendsAlwaysUseTheOpaqueFriendColor() {
        assertEquals(0xFF61D17B, ChamsColorPolicy.colorFor(true, FRIEND, true, true, 0.1D, BASE));
        assertEquals(0xFF61D17B, ChamsColorPolicy.colorFor(true, FRIEND, false, true, 1.0D, BASE));
    }

    @Test
    void baseColorIsUsedWhenHealthColorIsOff() {
        assertEquals(0xFFFF4D4D, ChamsColorPolicy.colorFor(false, FRIEND, false, true, 0.5D, BASE));
    }

    @Test
    void nonLivingTargetsFallBackToTheBaseColorEvenWithHealthColor() {
        assertEquals(0xFFFF4D4D, ChamsColorPolicy.colorFor(false, FRIEND, true, false, 0.5D, BASE));
    }

    @Test
    void healthColorInterpolatesGreenToRed() {
        assertEquals(0xFF00FF00, ChamsColorPolicy.colorFor(false, FRIEND, true, true, 1.0D, BASE));
        assertEquals(0xFFFF0000, ChamsColorPolicy.colorFor(false, FRIEND, true, true, 0.0D, BASE));
        assertEquals(0xFF808000, ChamsColorPolicy.colorFor(false, FRIEND, true, true, 0.5D, BASE));
    }

    @Test
    void healthGradientClampsOutOfRangeAndNonFiniteFractions() {
        assertEquals(0xFF00FF00, ChamsColorPolicy.healthGradient(1.5D));
        assertEquals(0xFFFF0000, ChamsColorPolicy.healthGradient(-0.5D));
        assertEquals(0xFFFF0000, ChamsColorPolicy.healthGradient(Double.NaN));
    }

    @Test
    void alphaIsAlwaysForcedOpaque() {
        assertEquals(0xFF123456, ChamsColorPolicy.colorFor(false, FRIEND, false, false, 1.0D, 0x00123456));
        assertEquals(0xFF654321, ChamsColorPolicy.colorFor(true, 0x00654321, false, false, 1.0D, BASE));
    }
}
