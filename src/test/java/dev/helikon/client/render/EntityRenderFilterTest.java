package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntityRenderFilterTest {
    private static final EntityRenderFilter.Options OPTIONS = new EntityRenderFilter.Options(
            true, false, true, false, false, true, 16.0D
    );

    @Test
    void respectsCategoryFriendSelfAndDistanceGates() {
        assertTrue(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PLAYER, false, false, 255.9D));
        assertFalse(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.HOSTILE, false, false, 4.0D));
        assertTrue(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PLAYER, true, false, 4.0D));
        assertFalse(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PLAYER, false, true, 4.0D));
        assertFalse(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PASSIVE, false, false, 256.1D));
    }

    @Test
    void rejectsInvalidDistanceValues() {
        assertFalse(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PLAYER, false, false,
                Double.NaN));
        assertFalse(EntityRenderFilter.shouldRender(OPTIONS, EntityRenderFilter.EntityType.PLAYER, false, false, -1.0D));
    }
}
