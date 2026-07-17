package dev.helikon.client.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalGlossaryTest {
    @Test
    void translatesExactCaseInsensitiveEntriesAndRejectsMalformedMappings() {
        assertEquals("Bonjour", LocalGlossary.translate("HELLO", "hello=Bonjour;invalid;bad=two=equals").orElseThrow());
        assertTrue(LocalGlossary.translate("unknown", "hello=Bonjour").isEmpty());
        assertFalse(LocalGlossary.parse("hello=Bonjour;hello=Salut").get("hello").equals("Salut"));
    }
}
