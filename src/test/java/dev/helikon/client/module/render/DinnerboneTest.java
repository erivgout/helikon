package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DinnerboneTest {
    @Test
    void flipsOnlyEnabledSelectedLivingCategories() {
        Dinnerbone module = new Dinnerbone();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);

        assertFalse(module.shouldFlip(EntityRenderFilter.EntityType.PLAYER));
        registry.setEnabled(module, true);
        assertTrue(module.shouldFlip(EntityRenderFilter.EntityType.PLAYER));
        assertTrue(module.shouldFlip(EntityRenderFilter.EntityType.HOSTILE));
        assertFalse(module.shouldFlip(EntityRenderFilter.EntityType.PASSIVE));
        assertFalse(module.shouldFlip(EntityRenderFilter.EntityType.ITEM));

        booleanSetting(module, "passive").set(true);
        assertTrue(module.shouldFlip(EntityRenderFilter.EntityType.PASSIVE));
    }

    private static BooleanSetting booleanSetting(Dinnerbone module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
