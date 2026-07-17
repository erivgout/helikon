package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastPlaceTest {
    @Test
    void reducesOnlyAnExistingCooldownForAHeldMatchingItem() {
        FastPlace fastPlace = enabledModule(new FakeCooldownAccess(4));

        assertEquals(new FastPlace.Action(true, 0), fastPlace.update(true, true, 4));
        assertEquals(FastPlace.Action.none(), fastPlace.update(true, true, 0));
        assertEquals(FastPlace.Action.none(), fastPlace.update(false, true, 4));
        assertEquals(FastPlace.Action.none(), fastPlace.update(true, false, 4));
    }

    @Test
    void respectsItemFilterAndSafeMinimumDelay() {
        FastPlace fastPlace = enabledModule(new FakeCooldownAccess(4));
        enumSetting(fastPlace, "item_filter").set(FastPlace.ItemFilter.NON_BLOCKS);
        numberSetting(fastPlace, "use_delay").set(0.0);
        numberSetting(fastPlace, "safe_minimum_delay").set(2.0);

        assertEquals(FastPlace.Action.none(), fastPlace.update(true, true, 4));
        assertEquals(new FastPlace.Action(true, 2), fastPlace.update(true, false, 4));
    }

    @Test
    void rejectsInvalidCooldownFacts() {
        assertThrows(IllegalArgumentException.class, () -> enabledModule(new FakeCooldownAccess(4)).update(true, true, -1));
    }

    @Test
    void restoresOnlyItsUnchangedCooldownWhenDisabled() {
        FakeCooldownAccess access = new FakeCooldownAccess(4);
        FastPlace fastPlace = enabledModule(access);

        assertEquals(new FastPlace.Action(true, 0), fastPlace.tick(true, true));
        assertEquals(0, access.delay());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fastPlace);
        registry.setEnabled(fastPlace, false);
        assertEquals(4, access.delay());
    }

    @Test
    void doesNotOverwriteACooldownChangedAfterItsLastReduction() {
        FakeCooldownAccess access = new FakeCooldownAccess(4);
        FastPlace fastPlace = enabledModule(access);
        fastPlace.tick(true, true);
        access.setDelay(3);

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fastPlace);
        registry.setEnabled(fastPlace, false);
        assertEquals(3, access.delay());
    }

    private static FastPlace enabledModule(FakeCooldownAccess access) {
        FastPlace fastPlace = new FastPlace(access);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fastPlace);
        registry.setEnabled(fastPlace, true);
        return fastPlace;
    }

    @SuppressWarnings("unchecked")
    private static EnumSetting<FastPlace.ItemFilter> enumSetting(FastPlace module, String id) {
        return (EnumSetting<FastPlace.ItemFilter>) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static NumberSetting numberSetting(FastPlace module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static final class FakeCooldownAccess implements FastPlace.CooldownAccess {
        private int delay;

        private FakeCooldownAccess(int delay) {
            this.delay = delay;
        }

        @Override
        public int delay() {
            return delay;
        }

        @Override
        public void setDelay(int value) {
            delay = value;
        }
    }
}
