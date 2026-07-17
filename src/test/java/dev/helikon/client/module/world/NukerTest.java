package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.player.ToolCandidate;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NukerTest {
    private static final Nuker.Target STONE_NEAR = new Nuker.Target(1, 64, 0, "minecraft:stone", 1.0D, true);
    private static final Nuker.Target STONE_FAR = new Nuker.Target(3, 64, 0, "minecraft:stone", 9.0D, true);
    private static final Nuker.Target DIRT_NEAR = new Nuker.Target(0, 64, 1, "minecraft:dirt", 1.0D, true);

    @Test
    void requiresAnEnabledHeldAttackAndExplicitWhitelist() {
        Nuker module = new Nuker();
        List<Nuker.Target> candidates = List.of(STONE_NEAR, DIRT_NEAR);
        assertTrue(module.selectTargets(new Nuker.Context(false, true), candidates).isEmpty());
        assertFalse(module.shouldScan(new Nuker.Context(false, true)));

        enable(module);
        assertTrue(module.selectTargets(new Nuker.Context(false, true), candidates).isEmpty());
        assertFalse(module.shouldScan(new Nuker.Context(false, true)));
        stringSetting(module, "whitelist").set("minecraft:stone");
        assertTrue(module.shouldScan(new Nuker.Context(false, true)));
        assertFalse(module.shouldScan(new Nuker.Context(true, true)));
        assertFalse(module.shouldScan(new Nuker.Context(false, false)));
        assertEquals(List.of(STONE_NEAR), module.selectTargets(new Nuker.Context(false, true), candidates));
        assertTrue(module.selectTargets(new Nuker.Context(true, true), candidates).isEmpty());
        assertTrue(module.selectTargets(new Nuker.Context(false, false), candidates).isEmpty());
    }

    @Test
    void enforcesLineOfSightDistanceFiltersAndTwoActionHardCap() {
        Nuker module = new Nuker();
        enable(module);
        stringSetting(module, "whitelist").set("minecraft:stone");
        numberSetting(module, "radius").set(2.0D);
        numberSetting(module, "blocks_per_tick").set(2.0D);
        numberSetting(module, "safety_limit").set(2.0D);

        assertEquals(List.of(STONE_NEAR), module.selectTargets(new Nuker.Context(false, true),
                List.of(STONE_FAR, STONE_NEAR, new Nuker.Target(2, 64, 0, "minecraft:stone", 4.0D, false))));
        booleanSetting(module, "line_of_sight").set(false);
        assertEquals(List.of(STONE_NEAR, new Nuker.Target(2, 64, 0, "minecraft:stone", 4.0D, false)),
                module.selectTargets(new Nuker.Context(false, true),
                        List.of(STONE_FAR, STONE_NEAR, new Nuker.Target(2, 64, 0, "minecraft:stone", 4.0D, false))));
    }

    @Test
    void restoresOnlyAHotbarSlotItStillOwns() {
        Nuker module = new Nuker();
        enable(module);
        stringSetting(module, "whitelist").set("minecraft:stone");
        List<ToolCandidate> tools = List.of(
                new ToolCandidate(0, 1.0D, false, 100),
                new ToolCandidate(3, 8.0D, true, 100));
        assertEquals(new Nuker.ToolAction(Nuker.ToolActionType.SELECT, 3), module.toolAction(true, 0, tools));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, false);
        assertEquals(new Nuker.ToolAction(Nuker.ToolActionType.RESTORE, 0), module.toolAction(false, 3, List.of()));
    }

    @Test
    void computesBoundedFiniteTargetRotation() {
        NukerRotation.Rotation rotation = NukerRotation.toward(0.5D, 65.5D, 0.5D, 1, 64, 0);
        assertEquals(-90.0F, rotation.yaw());
        assertEquals(45.0F, rotation.pitch());
    }

    private static void enable(Nuker module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
    }

    private static StringSetting stringSetting(Nuker module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Nuker module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static BooleanSetting booleanSetting(Nuker module, String id) {
        return (BooleanSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
