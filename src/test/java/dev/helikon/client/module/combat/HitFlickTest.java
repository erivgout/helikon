package dev.helikon.client.module.combat;

import dev.helikon.client.combat.HitFlickPolicy;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HitFlickTest {
    @Test
    void policyFlicksReverseAndSideAnglesRelativeToTargetDirection() {
        // Reverse faces directly away from the target so the bonus knockback pulls it inward.
        assertEquals(180.0F, Math.abs(HitFlickPolicy.flickedYaw(0.0F, HitFlickPolicy.Mode.REVERSE, 30.0D)), 0.0001F);
        assertEquals(-30.0F, HitFlickPolicy.flickedYaw(0.0F, HitFlickPolicy.Mode.LEFT, 30.0D), 0.0001F);
        assertEquals(135.0F, HitFlickPolicy.flickedYaw(90.0F, HitFlickPolicy.Mode.RIGHT, 45.0D), 0.0001F);
    }

    @Test
    void policyWrapsResultIntoSignedRangeAndRejectsInvalidFacts() {
        assertEquals(-145.0F, HitFlickPolicy.flickedYaw(170.0F, HitFlickPolicy.Mode.RIGHT, 45.0D), 0.0001F);
        assertThrows(IllegalArgumentException.class,
                () -> HitFlickPolicy.flickedYaw(0.0F, HitFlickPolicy.Mode.LEFT, 200.0D));
        assertThrows(IllegalArgumentException.class,
                () -> HitFlickPolicy.flickedYaw(Float.NaN, HitFlickPolicy.Mode.REVERSE, 30.0D));
    }

    @Test
    void moduleDefaultsOffExcludesFriendsAndOnlyFlicksWhileEnabled() {
        HitFlick hitFlick = new HitFlick();
        assertEquals("hit_flick", hitFlick.id());
        assertFalse(hitFlick.defaultEnabled());
        assertTrue(hitFlick.excludeFriends());
        assertTrue(hitFlick.flickYaw(0.0F).isEmpty());

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(hitFlick);
        registry.setEnabled(hitFlick, true);
        numberSetting(hitFlick, "side_angle").set(45.0D);
        enumSetting(hitFlick, "mode", HitFlickPolicy.Mode.class).set(HitFlickPolicy.Mode.RIGHT);
        Optional<Float> flicked = hitFlick.flickYaw(90.0F);
        assertTrue(flicked.isPresent());
        assertEquals(135.0F, flicked.orElseThrow(), 0.0001F);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> EnumSetting<E> enumSetting(HitFlick module, String id, Class<E> ignored) {
        return (EnumSetting<E>) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }

    private static NumberSetting numberSetting(HitFlick module, String id) {
        return (NumberSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
