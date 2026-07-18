package dev.helikon.client.module.world;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FastBreakTest {
    @Test
    void lowersOnlyAnExistingHeldTargetCooldownAndHonorsTheBlockFilter() {
        FastBreak fastBreak = enabled(new FakeCooldown(5));
        assertEquals(new FastBreak.Action(true, 0), fastBreak.update(true, true, "minecraft:stone", 5));
        assertEquals(1, fastBreak.extraDestroySteps(true, true, "minecraft:stone"));
        assertEquals(FastBreak.Action.none(), fastBreak.update(false, true, "minecraft:stone", 5));
        assertEquals(0, fastBreak.extraDestroySteps(false, true, "minecraft:stone"));
        assertEquals(FastBreak.Action.none(), fastBreak.update(true, false, "minecraft:stone", 5));

        stringSetting(fastBreak, "blocks").set("minecraft:stone;minecraft:deepslate");
        numberSetting(fastBreak, "break_delay").set(2.0D);
        numberSetting(fastBreak, "speed_multiplier").set(5.0D);
        assertEquals(FastBreak.Action.none(), fastBreak.update(true, true, "minecraft:dirt", 5));
        assertEquals(new FastBreak.Action(true, 2), fastBreak.update(true, true, "minecraft:stone", 5));
        assertEquals(4, fastBreak.extraDestroySteps(true, true, "minecraft:stone"));
    }

    @Test
    void restoresOnlyItsUnchangedCooldown() {
        FakeCooldown cooldown = new FakeCooldown(4);
        FastBreak fastBreak = enabled(cooldown);
        fastBreak.tick(true, true, "minecraft:stone");
        assertEquals(0, cooldown.delay());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fastBreak);
        registry.setEnabled(fastBreak, false);
        assertEquals(4, cooldown.delay());
    }

    @Test
    void rejectsNegativeCooldownFacts() {
        assertThrows(IllegalArgumentException.class,
                () -> enabled(new FakeCooldown(1)).update(true, true, "minecraft:stone", -1));
    }

    private static FastBreak enabled(FakeCooldown cooldown) {
        FastBreak fastBreak = new FastBreak(cooldown);
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(fastBreak);
        registry.setEnabled(fastBreak, true);
        return fastBreak;
    }

    private static NumberSetting numberSetting(FastBreak module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static StringSetting stringSetting(FastBreak module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static final class FakeCooldown implements FastBreak.CooldownAccess {
        private int delay;

        private FakeCooldown(int delay) {
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
