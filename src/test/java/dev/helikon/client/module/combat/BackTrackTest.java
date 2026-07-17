package dev.helikon.client.module.combat;

import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.BooleanSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackTrackTest {
    @Test
    void identityIsAStableCombatModuleDisabledByDefault() {
        BackTrack backTrack = new BackTrack();
        assertEquals("backtrack", backTrack.id());
        assertEquals(ModuleCategory.COMBAT, backTrack.category());
        assertFalse(backTrack.defaultEnabled());
        assertEquals(120L, backTrack.delayMillis());
    }

    @Test
    void neverDelaysWhileDisabled() {
        BackTrack backTrack = new BackTrack();
        assertFalse(backTrack.shouldDelay(true, false, false, 1.0D));
    }

    @Test
    void delaysNonFriendPlayersInRangeButExcludesFriendsAndDistantEntities() {
        BackTrack backTrack = enabled(new BackTrack());
        assertTrue(backTrack.shouldDelay(true, false, false, 4.0D));
        assertFalse(backTrack.shouldDelay(true, false, false, 12.0D));
        assertFalse(backTrack.shouldDelay(true, false, true, 4.0D));
        assertFalse(backTrack.shouldDelay(true, false, false, -1.0D));
    }

    @Test
    void mobTypesFollowTheirTogglesAndDefaultOff() {
        BackTrack backTrack = enabled(new BackTrack());
        // Hostiles and passives are off by default; only players are delayed.
        assertFalse(backTrack.shouldDelay(false, true, false, 3.0D));
        assertFalse(backTrack.shouldDelay(false, false, false, 3.0D));

        booleanSetting(backTrack, "hostiles").set(true);
        booleanSetting(backTrack, "passive").set(true);
        assertTrue(backTrack.shouldDelay(false, true, false, 3.0D));
        assertTrue(backTrack.shouldDelay(false, false, false, 3.0D));
    }

    @Test
    void friendExclusionCanBeDisabled() {
        BackTrack backTrack = enabled(new BackTrack());
        booleanSetting(backTrack, "exclude_friends").set(false);
        assertTrue(backTrack.shouldDelay(true, false, true, 4.0D));
    }

    private static <T extends Module> T enabled(T module) {
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);
        return module;
    }

    private static BooleanSetting booleanSetting(Module module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id)).findFirst().orElseThrow();
    }
}
