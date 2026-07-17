package dev.helikon.client.module.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutoSignTest {
    @Test
    void defaultsOffAndAlwaysProducesFourBoundedLines() {
        AutoSign module = new AutoSign();
        assertFalse(module.isEnabled());
        assertEquals(List.of("Helikon", "", "", ""), module.fourLines());
        assertEquals(4, module.fourLines().size());
    }
}
