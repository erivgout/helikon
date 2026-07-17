package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityEspNativeOutlineTargetsBuilderTest {
    @Test
    void enforcesTheEntityCap() {
        EntityEspNativeOutlineTargetsBuilder builder = new EntityEspNativeOutlineTargetsBuilder(2, false);

        assertTrue(builder.offer(1, 0xFF102030));
        assertTrue(builder.offer(2, 0xFF102030));
        assertFalse(builder.offer(3, 0xFF102030));

        EntityEspNativeOutlineTargets targets = builder.build();
        assertEquals(2, targets.entityIds().size());
        assertFalse(targets.contains(3));
    }

    @Test
    void allowsReofferingAnAlreadySelectedEntityAtTheCap() {
        EntityEspNativeOutlineTargetsBuilder builder = new EntityEspNativeOutlineTargetsBuilder(1, false);

        assertTrue(builder.offer(5, 0));
        assertTrue(builder.offer(5, 0));
        assertEquals(1, builder.build().entityIds().size());
    }

    @Test
    void forcesShaderColorsOpaque() {
        EntityEspNativeOutlineTargetsBuilder builder = new EntityEspNativeOutlineTargetsBuilder(4, true);
        builder.offer(1, 0x0033AAFF);

        assertEquals(0xFF33AAFF, builder.build().shaderColorFor(1).orElseThrow());
    }

    @Test
    void glowModeRecordsNoShaderColors() {
        EntityEspNativeOutlineTargetsBuilder builder = new EntityEspNativeOutlineTargetsBuilder(4, false);
        builder.offer(1, 0xFF33AAFF);

        assertTrue(builder.build().shaderColorFor(1).isEmpty());
        assertTrue(builder.build().contains(1));
    }

    @Test
    void rejectsNonPositiveCaps() {
        assertThrows(IllegalArgumentException.class, () -> new EntityEspNativeOutlineTargetsBuilder(0, false));
    }
}
