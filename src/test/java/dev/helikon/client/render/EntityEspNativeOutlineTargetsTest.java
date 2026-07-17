package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityEspNativeOutlineTargetsTest {
    @Test
    void emptySnapshotContainsNothing() {
        EntityEspNativeOutlineTargets targets = EntityEspNativeOutlineTargets.empty();

        assertFalse(targets.contains(7));
        assertTrue(targets.shaderColorFor(7).isEmpty());
    }

    @Test
    void reportsMembershipAndOptionalShaderColors() {
        EntityEspNativeOutlineTargets targets = new EntityEspNativeOutlineTargets(
                Set.of(3, 9), Map.of(9, 0xFF112233));

        assertTrue(targets.contains(3));
        assertTrue(targets.contains(9));
        assertFalse(targets.contains(4));
        assertTrue(targets.shaderColorFor(3).isEmpty());
        assertEquals(0xFF112233, targets.shaderColorFor(9).orElseThrow());
    }

    @Test
    void rejectsShaderColorsForUnselectedEntities() {
        assertThrows(IllegalArgumentException.class,
                () -> new EntityEspNativeOutlineTargets(Set.of(1), Map.of(2, 0xFF000000)));
    }

    @Test
    void snapshotsAreImmutable() {
        EntityEspNativeOutlineTargets targets = new EntityEspNativeOutlineTargets(Set.of(1), Map.of());

        assertThrows(UnsupportedOperationException.class, () -> targets.entityIds().add(2));
        assertThrows(UnsupportedOperationException.class, () -> targets.shaderColors().put(1, 0));
    }
}
