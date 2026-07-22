package dev.helikon.client.module.miscellaneous;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class UpdateCheckerTest {
    @Test
    void isAnExplicitOptInModule() {
        UpdateChecker module = new UpdateChecker();

        assertFalse(module.defaultEnabled());
        assertFalse(module.isEnabled());
    }
}
