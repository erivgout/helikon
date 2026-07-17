package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockColorMapTest {
    @Test
    void parsesValidLocalBlockColorOverridesOnly() {
        var colors = BlockColorMap.parse("minecraft:diamond_ore=#66ccff;minecraft:gold_ore=#80FFD54F;bad=#ffffff");
        assertEquals(0xFF66CCFF, colors.get("minecraft:diamond_ore"));
        assertEquals(0x80FFD54F, colors.get("minecraft:gold_ore"));
        assertTrue(!colors.containsKey("bad"));
    }
}
