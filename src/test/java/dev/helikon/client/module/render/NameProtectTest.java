package dev.helikon.client.module.render;

import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class NameProtectTest {
    @Test
    void replacesBoundedWholeNameMatchesOnlyWhileEnabled() {
        NameProtect protect = new NameProtect();

        assertEquals("name_protect", protect.id());
        assertEquals("NameProtect", protect.name());
        assertEquals(ModuleCategory.RENDER, protect.category());
        assertFalse(protect.defaultEnabled());
        assertEquals("Hello Alice", protect.protect("Hello Alice", "Alice"));

        protect.enable();

        assertEquals("Hello Protected and Protected!", protect.protect("Hello Alice and ALICE!", "Alice"));
        assertEquals("Malice Alice_2", protect.protect("Malice Alice_2", "Alice"));
        protect.disable();
        assertEquals("Alice", protect.protect("Alice", "Alice"));
    }

    @Test
    void settingsControlCaseSubstringAliasAndReplacementCap() {
        NameProtect protect = new NameProtect();
        protect.enable();
        booleanSetting(protect, "case_sensitive").set(true);
        booleanSetting(protect, "whole_name_only").set(false);
        stringSetting(protect, "alias").set("Hidden");
        integerSetting(protect, "maximum_replacements").set(1);

        assertEquals("Hidden ALICE Alice_2", protect.protect("Alice ALICE Alice_2", "Alice"));
        assertEquals("******", protect.aliasFor("Hidden"));
    }

    @Test
    void aliasAndReplacementBoundsAreValidated() {
        NameProtect protect = new NameProtect();
        StringSetting alias = stringSetting(protect, "alias");
        IntegerSetting maximum = integerSetting(protect, "maximum_replacements");

        assertEquals(32, alias.maximumLength());
        assertEquals(1, maximum.minimum());
        assertEquals(64, maximum.maximum());
        assertThrows(IllegalArgumentException.class, () -> alias.set("x".repeat(33)));
        assertThrows(IllegalArgumentException.class, () -> maximum.set(65));
    }

    private static BooleanSetting booleanSetting(NameProtect module, String id) {
        return (BooleanSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static IntegerSetting integerSetting(NameProtect module, String id) {
        return (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static StringSetting stringSetting(NameProtect module, String id) {
        return (StringSetting) module.settings().stream()
                .filter(setting -> setting.id().equals(id))
                .findFirst()
                .orElseThrow();
    }
}
