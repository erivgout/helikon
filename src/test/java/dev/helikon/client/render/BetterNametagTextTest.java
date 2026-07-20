package dev.helikon.client.render;

import dev.helikon.client.module.render.BetterNametags;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BetterNametagTextTest {
    private static final BetterNametagText.Facts FACTS =
            new BetterNametagText.Facts("Alex", 18.0F, 20.0F, 12, 4.25D, "minecraft:diamond_sword");

    @Test
    void stacksOnlyTheEnabledLocalFactsAsRows() {
        assertEquals(List.of(
                new BetterNametagText.Line("Alex [Friend]", BetterNametagText.COLOR_FRIEND),
                new BetterNametagText.Line("18/20", BetterNametagText.COLOR_HEALTH_HIGH),
                new BetterNametagText.Line("Armor 12", BetterNametagText.COLOR_ARMOR),
                new BetterNametagText.Line("Diamond Sword", BetterNametagText.COLOR_ITEM),
                new BetterNametagText.Line("4.3m", BetterNametagText.COLOR_DISTANCE)
        ), BetterNametagText.lines(FACTS, new BetterNametags.Options(true, true, true, true, true, 64.0D), true));
        assertEquals(List.of(new BetterNametagText.Line("Alex", BetterNametagText.COLOR_NAME)),
                BetterNametagText.lines(FACTS, new BetterNametags.Options(false, false, false, false, false, 64.0D), true));
    }

    @Test
    void friendRowColorRequiresTheFriendStatusSetting() {
        BetterNametags.Options withoutFriendStatus = new BetterNametags.Options(false, false, false, false, false, 64.0D);
        assertEquals(List.of(new BetterNametagText.Line("Alex", BetterNametagText.COLOR_NAME)),
                BetterNametagText.lines(FACTS, withoutFriendStatus, true));
    }

    @Test
    void hidesEmptyArmorAndEmptyHand() {
        BetterNametagText.Facts unarmed = new BetterNametagText.Facts("Alex", 18.0F, 20.0F, 0, 4.25D, "minecraft:air");
        assertEquals(List.of(
                new BetterNametagText.Line("Alex", BetterNametagText.COLOR_NAME),
                new BetterNametagText.Line("4.3m", BetterNametagText.COLOR_DISTANCE)
        ), BetterNametagText.lines(unarmed, new BetterNametags.Options(false, true, true, true, false, 64.0D), false));
    }

    @Test
    void keepsNonVanillaItemNamespaces() {
        BetterNametagText.Facts modded = new BetterNametagText.Facts("Alex", 18.0F, 20.0F, 0, 4.25D, "examplemod:wrench");
        assertEquals(List.of(
                new BetterNametagText.Line("Alex", BetterNametagText.COLOR_NAME),
                new BetterNametagText.Line("examplemod:wrench", BetterNametagText.COLOR_ITEM)
        ), BetterNametagText.lines(modded, new BetterNametags.Options(false, false, false, true, false, 64.0D), false));
    }

    @Test
    void stacksTheNameRowTopmost() {
        assertEquals(3, BetterNametagText.stackOffset(0, 4));
        assertEquals(0, BetterNametagText.stackOffset(3, 4));
        assertEquals(0, BetterNametagText.stackOffset(0, 1));
    }

    @Test
    void leavesAVisibleGapBetweenEveryRenderedRow() {
        double glyphHeight = BetterNametagText.worldLineSpacing(0.32F, 9, 0);
        double rowSpacing = BetterNametagText.worldLineSpacing(0.32F, 9, 2);

        assertEquals(0.288D, glyphHeight, 0.000_001D);
        assertEquals(0.352D, rowSpacing, 0.000_001D);
        assertTrue(rowSpacing > glyphHeight);
    }

    @Test
    void omitsPointZeroButKeepsRealFractionalHealth() {
        assertEquals("20/20", BetterNametagText.formatHealth(20.0F, 20.0F));
        assertEquals("19.5/20", BetterNametagText.formatHealth(19.5F, 20.0F));
    }

    @Test
    void rejectsInvalidLineLayoutInputs() {
        assertThrows(IllegalArgumentException.class, () -> BetterNametagText.worldLineSpacing(0.0F, 9, 2));
        assertThrows(IllegalArgumentException.class, () -> BetterNametagText.worldLineSpacing(0.32F, 0, 2));
        assertThrows(IllegalArgumentException.class, () -> BetterNametagText.worldLineSpacing(0.32F, 9, -1));
    }

    @Test
    void colorsHealthByRemainingFraction() {
        assertEquals(BetterNametagText.COLOR_HEALTH_HIGH, BetterNametagText.healthColor(1.0F));
        assertEquals(BetterNametagText.COLOR_HEALTH_HIGH, BetterNametagText.healthColor(2.0F / 3.0F));
        assertEquals(BetterNametagText.COLOR_HEALTH_MEDIUM, BetterNametagText.healthColor(0.5F));
        assertEquals(BetterNametagText.COLOR_HEALTH_LOW, BetterNametagText.healthColor(0.2F));
    }
}
