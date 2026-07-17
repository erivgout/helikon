package dev.helikon.client.module.combat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AntiFireballTest {
    @Test
    void disabledModuleSelectsNothing() {
        AntiFireball module = new AntiFireball();
        assertTrue(module.selectTarget(0L, List.of(observation("a", 2.0D, true, true))).isEmpty());
    }

    @Test
    void selectsNearestApproachingVisibleInRangeFireball() {
        AntiFireball module = enabled(new AntiFireball());
        AntiFireball.FireballObservation far = observation("far", 5.0D, true, true);
        AntiFireball.FireballObservation near = observation("near", 2.0D, true, true);
        AntiFireball.FireballObservation retreating = observation("away", 1.0D, false, true);
        AntiFireball.FireballObservation blocked = new AntiFireball.FireballObservation("blocked",
                AntiFireball.FireballKind.GHAST, 1.0D, true, false);
        AntiFireball.FireballObservation outOfRange = observation("outOfRange", 5.5D, true, true);

        Optional<String> selected = module.selectTarget(0L,
                List.of(far, near, retreating, blocked, outOfRange));
        assertEquals("near", selected.orElseThrow());
    }

    @Test
    void honorsDelayBetweenAttacks() {
        AntiFireball module = enabled(new AntiFireball());
        numberSetting(module, "delay_ticks").set(3.0D);
        List<AntiFireball.FireballObservation> targets = List.of(observation("a", 2.0D, true, true));

        assertEquals("a", module.selectTarget(0L, targets).orElseThrow());
        assertTrue(module.selectTarget(1L, targets).isEmpty());
        assertTrue(module.selectTarget(2L, targets).isEmpty());
        assertEquals("a", module.selectTarget(3L, targets).orElseThrow());
    }

    @Test
    void requireApproachingCanBeDisabledAndKindsAreSelectable() {
        AntiFireball module = enabled(new AntiFireball());
        booleanSetting(module, "require_approaching").set(false);
        AntiFireball.FireballObservation retreating = observation("away", 2.0D, false, true);
        assertEquals("away", module.selectTarget(0L, List.of(retreating)).orElseThrow());

        AntiFireball fresh = enabled(new AntiFireball());
        booleanSetting(fresh, "ghast_fireballs").set(false);
        AntiFireball.FireballObservation ghast = new AntiFireball.FireballObservation("ghast",
                AntiFireball.FireballKind.GHAST, 2.0D, true, true);
        AntiFireball.FireballObservation blaze = new AntiFireball.FireballObservation("blaze",
                AntiFireball.FireballKind.BLAZE, 3.0D, true, true);
        assertEquals("blaze", fresh.selectTarget(0L, List.of(ghast, blaze)).orElseThrow());
    }

    @Test
    void disableResetsCooldown() {
        ModuleRegistry registry = new ModuleRegistry();
        AntiFireball module = new AntiFireball();
        registry.register(module);
        registry.setEnabled(module, true);
        numberSetting(module, "delay_ticks").set(20.0D);
        List<AntiFireball.FireballObservation> targets = List.of(observation("a", 2.0D, true, true));
        assertEquals("a", module.selectTarget(5L, targets).orElseThrow());
        assertTrue(module.selectTarget(6L, targets).isEmpty());

        registry.setEnabled(module, false);
        registry.setEnabled(module, true);
        assertFalse(module.selectTarget(6L, targets).isEmpty());
    }

    private static AntiFireball.FireballObservation observation(String id, double distance, boolean approaching,
                                                                boolean lineOfSight) {
        return new AntiFireball.FireballObservation(id, AntiFireball.FireballKind.GHAST, distance, approaching,
                lineOfSight);
    }

    private static AntiFireball enabled(AntiFireball module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static NumberSetting numberSetting(AntiFireball module, String id) {
        return (NumberSetting) setting(module, id);
    }

    private static BooleanSetting booleanSetting(AntiFireball module, String id) {
        return (BooleanSetting) setting(module, id);
    }

    private static Setting<?> setting(AntiFireball module, String id) {
        return module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
