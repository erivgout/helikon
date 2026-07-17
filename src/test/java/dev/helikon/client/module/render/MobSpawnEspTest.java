package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSpawnEspTest {
    @Test
    void defaultsRecognizeDarkTwoHighSpaceOnARepresentativeHostileFloor() {
        MobSpawnEsp esp = new MobSpawnEsp();

        assertEquals("mob_spawn_esp", esp.id());
        assertEquals("MobSpawnESP", esp.name());
        assertEquals(ModuleCategory.RENDER, esp.category());
        assertFalse(esp.defaultEnabled());
        assertFalse(esp.isEnabled());
        assertTrue(esp.shouldMark(sample(true, true, true, 0, 7, false)));
        assertFalse(esp.shouldMark(sample(false, true, true, 0, 7, false)));
        assertFalse(esp.shouldMark(sample(true, true, false, 0, 7, false)));
        assertFalse(esp.shouldMark(sample(true, true, true, 1, 7, false)));
        assertFalse(esp.shouldMark(sample(true, true, true, 0, 8, false)));
        assertFalse(esp.shouldMark(sample(true, true, true, 0, 7, true)));
        assertFalse(esp.shouldMark(new MobSpawnEsp.SpawnSample(
                1, 64, 0, true, true, true, 0, 7, false, 1_000.0D)));
        assertTrue(esp.withinCurrentRange(new MobSpawnEsp.Marker(0, 64, 0), 0.0D, 64.0D, 0.0D));
        assertFalse(esp.withinCurrentRange(new MobSpawnEsp.Marker(40, 64, 0), 0.0D, 64.0D, 0.0D));
    }

    @Test
    void lightThresholdsRejectValuesOutsideMinecraftLightBounds() {
        MobSpawnEsp esp = new MobSpawnEsp();
        IntegerSetting blockLight = (IntegerSetting) esp.settings().stream()
                .filter(setting -> setting.id().equals("maximum_block_light"))
                .findFirst()
                .orElseThrow();
        IntegerSetting skyLight = (IntegerSetting) esp.settings().stream()
                .filter(setting -> setting.id().equals("maximum_sky_light"))
                .findFirst()
                .orElseThrow();

        assertEquals(0, blockLight.minimum());
        assertEquals(15, blockLight.maximum());
        assertEquals(0, skyLight.minimum());
        assertEquals(15, skyLight.maximum());
        assertThrows(IllegalArgumentException.class, () -> blockLight.set(-1));
        assertThrows(IllegalArgumentException.class, () -> skyLight.set(16));
    }

    @Test
    void optionalPlayerDistanceGateUsesVanillasTwentyFourBlockExclusion() {
        MobSpawnEsp esp = new MobSpawnEsp();
        BooleanSetting respectDistance = (BooleanSetting) esp.settings().stream()
                .filter(setting -> setting.id().equals("respect_player_distance"))
                .findFirst()
                .orElseThrow();
        respectDistance.set(true);

        assertFalse(esp.shouldMark(new MobSpawnEsp.SpawnSample(
                0, 64, 0, true, true, true, 0, 7, false, 23.9D * 23.9D)));
        assertTrue(esp.shouldMark(new MobSpawnEsp.SpawnSample(
                0, 64, 0, true, true, true, 0, 7, false, 24.0D * 24.0D)));
    }

    @Test
    void disableClearsTransientScannerState() {
        MobSpawnEsp esp = new MobSpawnEsp();
        boolean[] cleared = {false};
        esp.setCacheClearer(() -> cleared[0] = true);

        esp.enable();
        esp.disable();

        assertTrue(cleared[0]);
    }

    private static MobSpawnEsp.SpawnSample sample(
            boolean feetAir,
            boolean headAir,
            boolean validFloor,
            int blockLight,
            int skyLight,
            boolean peaceful
    ) {
        return new MobSpawnEsp.SpawnSample(
                0, 64, 0, feetAir, headAir, validFloor, blockLight, skyLight, peaceful, 1_000.0D);
    }
}
