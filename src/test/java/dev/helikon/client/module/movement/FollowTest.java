package dev.helikon.client.module.movement;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FollowTest {
    @Test
    void picksNearestConfiguredTargetAndComputesBoundedVelocity() {
        Follow follow = enabled(new Follow());
        stringSetting(follow, "target_name").set("Alex");

        assertEquals("follow", follow.id());
        assertEquals(ModuleCategory.MOVEMENT, follow.category());
        assertFalse(follow.defaultEnabled());
        HorizontalVelocity velocity = follow.velocity(context(false, List.of(
                new Follow.Target("Alex", 10.0D, 10.0D, 0.0D),
                new Follow.Target("Alex", 5.0D, 0.0D, 5.0D),
                new Follow.Target("Steve", 3.0D, 3.0D, 0.0D)
        ))).orElseThrow();
        assertEquals(0.0D, velocity.x(), 0.0001D);
        assertEquals(0.20D, velocity.z(), 0.0001D);
    }

    @Test
    void respectsDistancesSettingsAndUnsafeContexts() {
        Follow follow = enabled(new Follow());
        stringSetting(follow, "target_name").set("Alex");
        assertTrue(follow.velocity(context(false, List.of(new Follow.Target("Alex", 2.0D, 2.0D, 0.0D)))).isEmpty());
        assertTrue(follow.velocity(context(true, List.of(new Follow.Target("Alex", 5.0D, 5.0D, 0.0D)))).isEmpty());
        numberSetting(follow, "range").set(4.0D);
        assertTrue(follow.velocity(context(false, List.of(new Follow.Target("Alex", 5.0D, 5.0D, 0.0D)))).isEmpty());
        assertEquals(0.20D, numberSetting(follow, "speed").value());
        assertEquals(2.0D, numberSetting(follow, "stop_distance").value());
    }

    private static Follow.Context context(boolean screenOpen, List<Follow.Target> targets) {
        return new Follow.Context(screenOpen, false, false, false, targets);
    }

    private static Follow enabled(Follow module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static StringSetting stringSetting(Follow module, String id) {
        return (StringSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(Follow module, String id) {
        return (NumberSetting) module.settings().stream().filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
