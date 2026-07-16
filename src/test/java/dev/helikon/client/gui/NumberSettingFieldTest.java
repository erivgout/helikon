package dev.helikon.client.gui;

import dev.helikon.client.setting.NumberSetting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberSettingFieldTest {
    private static NumberSetting setting() {
        return new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0);
    }

    @Test
    void formatUsesIntegerTextForWholeValues() {
        assertEquals("5", NumberSettingField.format(5.0));
        assertEquals("0", NumberSettingField.format(0.0));
        assertEquals("-3", NumberSettingField.format(-3.0));
    }

    @Test
    void formatKeepsDecimalText() {
        assertEquals("7.5", NumberSettingField.format(7.5));
    }

    @Test
    void validTextUpdatesTheSetting() {
        NumberSetting setting = setting();
        assertTrue(NumberSettingField.tryApply(setting, "7.5"));
        assertEquals(7.5, setting.value());
    }

    @Test
    void surroundingWhitespaceIsAccepted() {
        NumberSetting setting = setting();
        assertTrue(NumberSettingField.tryApply(setting, "  4 "));
        assertEquals(4.0, setting.value());
    }

    @Test
    void unparsableTextKeepsTheCurrentValue() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingField.tryApply(setting, "abc"));
        assertFalse(NumberSettingField.tryApply(setting, ""));
        assertFalse(NumberSettingField.tryApply(setting, null));
        assertEquals(2.0, setting.value());
    }

    @Test
    void outOfRangeValuesAreRejected() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingField.tryApply(setting, "10.5"));
        assertFalse(NumberSettingField.tryApply(setting, "-0.1"));
        assertEquals(2.0, setting.value());
    }

    @Test
    void nonFiniteValuesAreRejected() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingField.tryApply(setting, "NaN"));
        assertFalse(NumberSettingField.tryApply(setting, "Infinity"));
        assertEquals(2.0, setting.value());
    }
}
