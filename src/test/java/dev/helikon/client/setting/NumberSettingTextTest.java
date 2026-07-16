package dev.helikon.client.setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberSettingTextTest {
    private static NumberSetting setting() {
        return new NumberSetting("amount", "Amount", "A test number.", 2.0, 0.0, 10.0);
    }

    @Test
    void formatUsesIntegerTextForWholeValues() {
        assertEquals("5", NumberSettingText.format(5.0));
        assertEquals("0", NumberSettingText.format(0.0));
        assertEquals("-3", NumberSettingText.format(-3.0));
    }

    @Test
    void formatKeepsDecimalText() {
        assertEquals("7.5", NumberSettingText.format(7.5));
    }

    @Test
    void validTextUpdatesTheSetting() {
        NumberSetting setting = setting();
        assertTrue(NumberSettingText.tryApply(setting, "7.5"));
        assertEquals(7.5, setting.value());
    }

    @Test
    void surroundingWhitespaceIsAccepted() {
        NumberSetting setting = setting();
        assertTrue(NumberSettingText.tryApply(setting, "  4 "));
        assertEquals(4.0, setting.value());
    }

    @Test
    void unparsableTextKeepsTheCurrentValue() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingText.tryApply(setting, "abc"));
        assertFalse(NumberSettingText.tryApply(setting, ""));
        assertFalse(NumberSettingText.tryApply(setting, null));
        assertEquals(2.0, setting.value());
    }

    @Test
    void outOfRangeValuesAreRejected() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingText.tryApply(setting, "10.5"));
        assertFalse(NumberSettingText.tryApply(setting, "-0.1"));
        assertEquals(2.0, setting.value());
    }

    @Test
    void nonFiniteValuesAreRejected() {
        NumberSetting setting = setting();
        assertFalse(NumberSettingText.tryApply(setting, "NaN"));
        assertFalse(NumberSettingText.tryApply(setting, "Infinity"));
        assertEquals(2.0, setting.value());
    }
}
