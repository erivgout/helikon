package dev.helikon.client.module.player;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoEatTest {
    @Test
    void selectsPriorityFoodUsesTheNormalKeyAndRestoresItsOwnedSlot() {
        FakeUseKey useKey = new FakeUseKey();
        AutoEat autoEat = enabledModule(useKey);
        List<FoodCandidate> candidates = List.of(
                new FoodCandidate(1, "minecraft:bread", 5, 0.6F, false),
                new FoodCandidate(2, "minecraft:cooked_beef", 8, 0.8F, false)
        );

        AutoEat.Action begin = autoEat.tick(context(0, 10, 20.0F, candidates));
        assertEquals(new AutoEat.Action(2, true, false, -1), begin);
        assertEquals(true, useKey.down);

        AutoEat.Action finish = autoEat.tick(context(2, 20, 20.0F, candidates));
        assertEquals(new AutoEat.Action(-1, false, true, 0), finish);
        assertFalse(useKey.down);
    }

    @Test
    void honorsAvoidedFoodsAndCombatOrManualUseInterruption() {
        FakeUseKey useKey = new FakeUseKey();
        AutoEat autoEat = enabledModule(useKey);
        stringSetting(autoEat, "avoided_foods").set("minecraft:cooked_beef");
        List<FoodCandidate> candidates = List.of(
                new FoodCandidate(1, "minecraft:bread", 5, 0.6F, false),
                new FoodCandidate(2, "minecraft:cooked_beef", 8, 0.8F, false)
        );

        assertEquals(new AutoEat.Action(1, true, false, -1), autoEat.tick(context(0, 10, 20.0F, candidates)));
        assertEquals(true, useKey.down);

        AutoEat paused = enabledModule(new FakeUseKey());
        assertEquals(AutoEat.Action.none(), paused.tick(new AutoEat.Context(
                0, 10, 20.0F, true, false, false, false, false, candidates
        )));
        AutoEat userUsing = enabledModule(new FakeUseKey());
        assertEquals(AutoEat.Action.none(), userUsing.tick(new AutoEat.Context(
                0, 10, 20.0F, false, false, false, true, false, candidates
        )));
    }

    @Test
    void disableReleasesOnlyItsUseKeyAndRestoresLaterWithoutOverwritingManualSlotChange() {
        FakeUseKey useKey = new FakeUseKey();
        AutoEat autoEat = enabledModule(useKey);
        List<FoodCandidate> candidates = List.of(new FoodCandidate(1, "minecraft:bread", 5, 0.6F, false));
        autoEat.tick(context(0, 10, 20.0F, candidates));
        assertEquals(true, useKey.down);

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoEat);
        registry.setEnabled(autoEat, false);
        assertFalse(useKey.down);
        assertEquals(new AutoEat.Action(-1, false, false, 0), autoEat.update(context(1, 10, 20.0F, candidates)));

        AutoEat manuallyChanged = enabledModule(new FakeUseKey());
        manuallyChanged.tick(context(0, 10, 20.0F, candidates));
        assertEquals(new AutoEat.Action(-1, false, true, -1), manuallyChanged.tick(context(4, 10, 20.0F, candidates)));
    }

    @Test
    void handsOffUseToAPlayerPhysicalHoldInsteadOfCancellingIt() {
        FakeUseKey useKey = new FakeUseKey();
        AutoEat autoEat = enabledModule(useKey);
        List<FoodCandidate> candidates = List.of(new FoodCandidate(1, "minecraft:bread", 5, 0.6F, false));
        autoEat.tick(context(0, 10, 20.0F, candidates));
        useKey.physicallyDown = true;

        assertEquals(new AutoEat.Action(-1, false, true, 0), autoEat.tick(context(1, 20, 20.0F, candidates)));
        assertEquals(true, useKey.down);
    }

    @Test
    void permitsOnlyAlwaysEdibleFoodAtFullHungerWhenHealthIsLow() {
        AutoEat autoEat = enabledModule(new FakeUseKey());
        List<FoodCandidate> candidates = List.of(
                new FoodCandidate(1, "minecraft:cooked_beef", 8, 0.8F, false),
                new FoodCandidate(2, "minecraft:golden_apple", 4, 1.2F, true)
        );

        assertEquals(new AutoEat.Action(2, true, false, -1), autoEat.tick(context(0, 20, 6.0F, candidates)));
    }

    @Test
    void highestSaturationRanksActualGainRatherThanRawModifier() {
        AutoEat autoEat = enabledModule(new FakeUseKey());
        enumSetting(autoEat, "food_priority").set(AutoEat.FoodPriority.HIGHEST_SATURATION);
        List<FoodCandidate> candidates = List.of(
                new FoodCandidate(1, "minecraft:bread", 5, 1.0F, false),
                new FoodCandidate(2, "minecraft:cooked_beef", 8, 0.8F, false)
        );

        assertEquals(new AutoEat.Action(2, true, false, -1), autoEat.tick(context(0, 10, 20.0F, candidates)));
    }

    @SuppressWarnings("unchecked")
    private static dev.helikon.client.setting.EnumSetting<AutoEat.FoodPriority> enumSetting(AutoEat module, String id) {
        return (dev.helikon.client.setting.EnumSetting<AutoEat.FoodPriority>) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static StringSetting stringSetting(AutoEat module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static AutoEat enabledModule(FakeUseKey useKey) {
        AutoEat autoEat = new AutoEat(useKey);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(autoEat);
        registry.setEnabled(autoEat, true);
        return autoEat;
    }

    private static AutoEat.Context context(int currentSlot, int hunger, float health, List<FoodCandidate> candidates) {
        return new AutoEat.Context(currentSlot, hunger, health, false, false, false, false, false, candidates);
    }

    private static final class FakeUseKey implements AutoEat.UseKeyAccess {
        private boolean down;
        private boolean physicallyDown;

        @Override
        public boolean isPhysicallyDown() {
            return physicallyDown;
        }

        @Override
        public void setDown(boolean value) {
            down = value;
        }
    }
}
