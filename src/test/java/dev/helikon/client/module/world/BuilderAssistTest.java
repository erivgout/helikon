package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuilderAssistTest {
    private static final BuilderPlan.Anchor ANCHOR = new BuilderPlan.Anchor(new BuildPoint(0, 64, 0),
            new BuildVector(1, 0, 0), new BuildVector(0, 0, 1));

    @Test
    void requestsOnlyOneReplaceablePositionWhenUseAndABlockArePresent() {
        BuilderAssist module = enabled();

        assertEquals(new BuildPoint(0, 64, 0), module.nextAction(0L,
                new BuilderAssist.Context(true, true, ANCHOR, Set.of(new BuildPoint(0, 64, 0)))).orElseThrow());
        assertFalse(module.nextAction(1L,
                new BuilderAssist.Context(true, true, ANCHOR, Set.of(new BuildPoint(0, 64, 0)))).isPresent());
        assertFalse(module.nextAction(4L,
                new BuilderAssist.Context(false, true, ANCHOR, Set.of(new BuildPoint(0, 64, 0)))).isPresent());
        assertFalse(module.nextAction(4L,
                new BuilderAssist.Context(true, false, ANCHOR, Set.of(new BuildPoint(0, 64, 0)))).isPresent());
    }

    @Test
    void nonRepeatingModeDoesNotSkipTheFirstBlockedPreviewPosition() {
        BuilderAssist module = enabled();
        booleanSetting(module, "repeat_placement").set(false);

        assertTrue(module.nextAction(0L, new BuilderAssist.Context(true, true, ANCHOR,
                Set.of(new BuildPoint(1, 64, 0)))).isEmpty());
    }

    private static BuilderAssist enabled() {
        BuilderAssist module = new BuilderAssist();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(BuilderAssist module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst()
                .orElseThrow();
    }
}
