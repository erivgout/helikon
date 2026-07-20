package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleRegistry;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterNametagsTest {
    @Test
    void replacesVanillaOnlyForEligibleRemotePlayersInRange() {
        BetterNametags nametags = new BetterNametags();
        assertFalse(nametags.replacesVanillaName(true, true, true, 16.0D));

        ModuleRegistry modules = new ModuleRegistry();
        modules.register(nametags);
        modules.setEnabled(nametags, true);

        assertTrue(nametags.replacesVanillaName(true, true, true, 16.0D));
        assertFalse(nametags.replacesVanillaName(false, true, true, 16.0D));
        assertFalse(nametags.replacesVanillaName(true, false, true, 16.0D));
        assertFalse(nametags.replacesVanillaName(true, true, false, 16.0D));
        assertFalse(nametags.replacesVanillaName(true, true, true, 65.0D * 65.0D));
    }
}
