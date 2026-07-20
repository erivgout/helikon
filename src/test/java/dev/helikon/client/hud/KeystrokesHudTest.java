package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KeystrokesHudTest {
    @Test
    void keepsConfiguredKeyNamesCompact() {
        assertEquals("W", KeystrokesHud.keyName("w"));
        assertEquals("CTL", KeystrokesHud.keyName("Left Control"));
        assertEquals("?", KeystrokesHud.keyName(" "));
    }
}
