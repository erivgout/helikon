package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AirPlaceTest {
    // Looking straight down the +X axis from an eye at (0.5, 65, 0.5).
    private static final AirPlace.Ray FORWARD_X = new AirPlace.Ray(0.5D, 65.0D, 0.5D, 1.0D, 0.0D, 0.0D);

    @Test
    void planFillsTheBlockAtRangeAlongTheCrosshairWithAFaceTowardTheViewer() {
        AirPlace module = enabled();

        AirPlace.Placement placement = module.plan(0L, new AirPlace.Input(true, true, false, FORWARD_X)).orElseThrow();

        // Default range 4.5 -> point x = 0.5 + 4.5 = 5.0 -> block x = 5.
        assertEquals(new BuildPoint(5, 65, 0), placement.block());
        // Looking toward +X, the clicked face points back toward the viewer (-X).
        assertEquals(new BuildVector(-1, 0, 0), placement.face());
        // Hit point sits on that face plane, centered on the other two axes.
        assertEquals(5.0D, placement.hitX(), 1.0e-9D);
        assertEquals(65.5D, placement.hitY(), 1.0e-9D);
        assertEquals(0.5D, placement.hitZ(), 1.0e-9D);
    }

    @Test
    void planIsGatedByUseHeldHeldBlockAndEnabledState() {
        AirPlace module = enabled();

        assertFalse(module.plan(0L, new AirPlace.Input(false, true, false, FORWARD_X)).isPresent());
        assertFalse(module.plan(0L, new AirPlace.Input(true, false, false, FORWARD_X)).isPresent());

        module.disable();
        assertFalse(module.plan(0L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
    }

    @Test
    void planRespectsTheDelayBetweenPlacements() {
        AirPlace module = enabled();
        numberSetting(module, "placement_delay_ticks").set(4.0D);

        assertTrue(module.plan(0L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
        assertFalse(module.plan(1L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
        assertFalse(module.plan(3L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
        assertTrue(module.plan(4L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
    }

    @Test
    void onlyWithoutTargetDefersToNormalPlacementWhenTheCrosshairIsOnABlock() {
        AirPlace module = enabled();

        assertTrue(booleanSetting(module, "only_without_target").value());
        assertFalse(module.plan(0L, new AirPlace.Input(true, true, true, FORWARD_X)).isPresent());

        booleanSetting(module, "only_without_target").set(false);
        assertTrue(module.plan(0L, new AirPlace.Input(true, true, true, FORWARD_X)).isPresent());
    }

    @Test
    void disableResetsTheDelaySoTheNextPlanCanFireImmediately() {
        AirPlace module = enabled();
        assertTrue(module.plan(10L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());

        module.disable();
        module.enable();

        assertTrue(module.plan(0L, new AirPlace.Input(true, true, false, FORWARD_X)).isPresent());
    }

    @Test
    void dominantFaceTowardPicksTheAxisMostOpposedToTheLookDirection() {
        assertEquals(new BuildVector(0, -1, 0), AirPlace.dominantFaceToward(0.1D, 0.9D, 0.2D));
        assertEquals(new BuildVector(0, 1, 0), AirPlace.dominantFaceToward(0.1D, -0.9D, 0.2D));
        assertEquals(new BuildVector(-1, 0, 0), AirPlace.dominantFaceToward(0.8D, 0.1D, 0.2D));
        assertEquals(new BuildVector(0, 0, 1), AirPlace.dominantFaceToward(0.2D, 0.1D, -0.8D));
    }

    private static AirPlace enabled() {
        AirPlace module = new AirPlace();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static NumberSetting numberSetting(AirPlace module, String id) {
        return (NumberSetting) setting(module, id);
    }

    private static BooleanSetting booleanSetting(AirPlace module, String id) {
        return (BooleanSetting) setting(module, id);
    }

    private static Setting<?> setting(AirPlace module, String id) {
        Optional<Setting<?>> found = module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst();
        return found.orElseThrow();
    }
}
