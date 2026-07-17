package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockInTest {
    private static final BuildPoint FEET = new BuildPoint(0, 64, 0);

    @Test
    void defaultPlanIsTheTwoLayerCardinalRing() {
        BlockIn module = enabled();

        List<BuildPoint> targets = module.targets(FEET);

        assertEquals(8, targets.size());
        assertTrue(targets.contains(new BuildPoint(1, 64, 0)));
        assertTrue(targets.contains(new BuildPoint(-1, 64, 0)));
        assertTrue(targets.contains(new BuildPoint(0, 64, 1)));
        assertTrue(targets.contains(new BuildPoint(0, 64, -1)));
        assertTrue(targets.contains(new BuildPoint(1, 65, 0)));
        assertFalse(targets.contains(new BuildPoint(1, 65, 1)));
        assertFalse(targets.contains(FEET));
    }

    @Test
    void cornersFloorAndRoofExtendThePlanWithoutOverlappingThePlayer() {
        BlockIn module = enabled();
        booleanSetting(module, "include_corners").set(true);
        booleanSetting(module, "floor").set(true);
        booleanSetting(module, "roof").set(true);

        List<BuildPoint> targets = module.targets(FEET);

        assertTrue(targets.contains(new BuildPoint(1, 64, 1)));
        assertTrue(targets.contains(new BuildPoint(0, 63, 0)));
        assertTrue(targets.contains(new BuildPoint(0, 66, 0)));
        assertFalse(targets.contains(FEET));
        // 4 cardinals + 4 corners over 2 layers, plus a floor and a roof.
        assertEquals(18, targets.size());
    }

    @Test
    void nextPlacementsReturnsOnlyReplaceableTargetsUpToTheTickCap() {
        BlockIn module = enabled();
        numberSetting(module, "blocks_per_tick").set(2.0D);
        Set<BuildPoint> replaceable = Set.of(new BuildPoint(1, 64, 0), new BuildPoint(-1, 64, 0),
                new BuildPoint(0, 64, 1));

        List<BuildPoint> first = module.nextPlacements(0L, new BlockIn.Context(FEET, replaceable));

        assertEquals(2, first.size());
        assertTrue(replaceable.containsAll(first));
    }

    @Test
    void placementDelayGatesTheNextCycle() {
        BlockIn module = enabled();
        numberSetting(module, "placement_delay_ticks").set(5.0D);
        Set<BuildPoint> replaceable = module.targets(FEET).stream().collect(Collectors.toSet());

        assertFalse(module.nextPlacements(0L, new BlockIn.Context(FEET, replaceable)).isEmpty());
        assertTrue(module.nextPlacements(4L, new BlockIn.Context(FEET, replaceable)).isEmpty());
        assertFalse(module.nextPlacements(5L, new BlockIn.Context(FEET, replaceable)).isEmpty());
    }

    @Test
    void disabledModulePlansNothing() {
        BlockIn module = new BlockIn();

        assertTrue(module.nextPlacements(0L, new BlockIn.Context(FEET,
                Set.of(new BuildPoint(1, 64, 0)))).isEmpty());
    }

    private static BlockIn enabled() {
        BlockIn module = new BlockIn();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(BlockIn module, String id) {
        return (BooleanSetting) setting(module, id);
    }

    private static NumberSetting numberSetting(BlockIn module, String id) {
        return (NumberSetting) setting(module, id);
    }

    private static dev.helikon.client.setting.Setting<?> setting(BlockIn module, String id) {
        return module.settings().stream().filter(s -> s.id().equals(id)).findFirst().orElseThrow();
    }
}
