package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyWorldModulesTest {
    @Test
    void actionModulesDefaultOffAndSelectOnlyTheirOwnFacts() {
        List<BoundedWorldAction> modules = List.of(new AutoFarm(), new BonemealAura(), new Excavator(),
                new Kaboom(), new Liquids(), new TillAura(), new TreeBot(), new Tunneller(),
                new VeinMiner(), new BuildRandom(), new GhostHand());
        assertTrue(modules.stream().allMatch(module -> !module.defaultEnabled()));

        assertSelected(new AutoFarm(), candidate(true, false, false, false, false, false, false, false, false));
        assertSelected(new BonemealAura(), candidate(false, true, false, false, false, false, false, false, false));
        assertSelected(new Excavator(), candidate(false, false, false, false, false, false, false, true, false));
        assertSelected(new Kaboom(), candidate(false, false, false, false, true, false, false, false, false));
        assertSelected(new Liquids(), candidate(false, false, false, true, false, false, false, false, false));
        assertSelected(new TillAura(), candidate(false, false, true, false, false, false, false, false, false));
        assertSelected(new TreeBot(), candidate(false, false, false, false, false, true, false, false, false));
        assertSelected(new Tunneller(), candidate(false, false, false, false, false, false, false, false, true));
        assertSelected(new VeinMiner(), candidate(false, false, false, false, false, false, true, false, false));
        assertSelected(new BuildRandom(), candidate(false, false, false, false, false, false, false, false, false, true));
        assertSelected(new GhostHand(), candidate(false, false, false, false, false, false, false, true, false));
    }

    @Test
    void cadenceAndDisableResetAreSharedAndBounded() {
        AutoFarm module = enabled(new AutoFarm());
        BoundedWorldAction.Candidate crop = candidate(true, false, false, false,
                false, false, false, false, false);
        assertTrue(module.select(0L, false, List.of(crop)).isPresent());
        module.markActed(0L);
        assertTrue(module.select(1L, false, List.of(crop)).isEmpty());
        module.disable();
        module.enable();
        assertTrue(module.select(0L, false, List.of(crop)).isPresent());
        assertTrue(module.select(0L, true, List.of(crop)).isEmpty());
    }

    @Test
    void autoFarmUsesPlantSpecificHarvestPaths() {
        AutoFarm module = new AutoFarm();

        assertEquals(AutoFarm.HarvestMode.BREAK_AND_REPLANT_BELOW,
                module.harvestMode("minecraft:wheat"));
        assertEquals(AutoFarm.HarvestMode.BREAK_AND_REPLANT_BELOW,
                module.harvestMode("minecraft:nether_wart"));
        assertEquals(AutoFarm.HarvestMode.PICK_IN_PLACE,
                module.harvestMode("minecraft:sweet_berry_bush"));
        assertEquals(AutoFarm.HarvestMode.BREAK_AND_REPLANT_COCOA,
                module.harvestMode("minecraft:cocoa"));
        assertEquals(AutoFarm.HarvestMode.BREAK_ABOVE_BASE,
                module.harvestMode("minecraft:sugar_cane"));
        assertEquals(AutoFarm.HarvestMode.BREAK_ABOVE_BASE,
                module.harvestMode("minecraft:bamboo"));
        assertEquals(AutoFarm.HarvestMode.BREAK_ABOVE_BASE,
                module.harvestMode("minecraft:cactus"));
        assertEquals(AutoFarm.HarvestMode.BREAK_FRUIT,
                module.harvestMode("minecraft:melon"));
        assertEquals(AutoFarm.HarvestMode.BREAK_FRUIT,
                module.harvestMode("minecraft:pumpkin"));
    }

    @Test
    void feedAuraSelectsNearestAcceptingAnimalAtCadence() {
        FeedAura module = enabled(new FeedAura());
        assertEquals(2, module.select(0L, false, true, List.of(
                new FeedAura.Candidate(1, 2.0D, false),
                new FeedAura.Candidate(2, 3.0D, true))).orElseThrow());
        module.markFed(0L);
        assertTrue(module.select(1L, false, true,
                List.of(new FeedAura.Candidate(2, 3.0D, true))).isEmpty());
        assertTrue(module.select(20L, false, false,
                List.of(new FeedAura.Candidate(2, 3.0D, true))).isEmpty());
        assertFalse(module.defaultEnabled());
    }

    private static void assertSelected(BoundedWorldAction module, BoundedWorldAction.Candidate candidate) {
        enabled(module);
        assertTrue(module.select(0L, false, List.of(candidate)).isPresent(), module.id());
    }

    private static BoundedWorldAction.Candidate candidate(boolean mature, boolean growable, boolean tillable,
                                                           boolean liquid, boolean tnt, boolean log, boolean ore,
                                                           boolean excavatable, boolean tunnel) {
        return candidate(mature, growable, tillable, liquid, tnt, log, ore, excavatable, tunnel, false);
    }

    private static BoundedWorldAction.Candidate candidate(boolean mature, boolean growable, boolean tillable,
                                                           boolean liquid, boolean tnt, boolean log, boolean ore,
                                                           boolean excavatable, boolean tunnel, boolean replaceable) {
        return new BoundedWorldAction.Candidate(0, 64, 0, "minecraft:test", 2.0D,
                mature, growable, tillable, liquid, tnt, log, ore, excavatable, tunnel, replaceable);
    }

    private static <T extends dev.helikon.client.module.Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }
}
