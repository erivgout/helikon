package dev.helikon.client.render;

import dev.helikon.client.module.render.BetterNametags;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BetterNametagTextTest {
    @Test
    void formatsOnlyTheEnabledLocalFacts() {
        BetterNametagText.Facts facts = new BetterNametagText.Facts("Alex", 18.0F, 20.0F, 12, 4.25D, "minecraft:diamond_sword");
        assertEquals("Alex [Friend] 18.0/20.0 A:12 4.3m minecraft:diamond_sword",
                BetterNametagText.format(facts, new BetterNametags.Options(true, true, true, true, true, 64.0D), true));
        assertEquals("Alex", BetterNametagText.format(facts,
                new BetterNametags.Options(false, false, false, false, false, 64.0D), true));
    }

    @Test
    void friendColorRequiresTheFriendStatusSetting() {
        assertEquals(0xFF80CBC4, MinecraftWorldVisualizationRenderer.nametagColor(
                new BetterNametags.Options(false, false, false, false, true, 64.0D), true));
        assertEquals(0xFFE5EDF5, MinecraftWorldVisualizationRenderer.nametagColor(
                new BetterNametags.Options(false, false, false, false, false, 64.0D), true));
    }
}
