package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OneClickFriendsTest {
    @Test
    void permitsTheGestureOnlyWhileTheRegisteredModuleIsEnabled() {
        OneClickFriends oneClickFriends = new OneClickFriends();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(oneClickFriends);

        assertFalse(oneClickFriends.allowsToggle());
        registry.setEnabled(oneClickFriends, true);
        assertTrue(oneClickFriends.allowsToggle());
        registry.setEnabled(oneClickFriends, false);
        assertFalse(oneClickFriends.allowsToggle());
    }
}
