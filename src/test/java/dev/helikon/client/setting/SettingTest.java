package dev.helikon.client.setting;

import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingTest {
    @Test
    void numberSettingRejectsValuesOutsideItsInclusiveRange() {
        NumberSetting setting = new NumberSetting("speed", "Speed", "Test speed.", 1.0, 0.0, 2.0);

        setting.set(1.5);

        assertEquals(1.5, setting.value());
        assertThrows(IllegalArgumentException.class, () -> setting.set(2.1));
        assertThrows(IllegalArgumentException.class, () -> setting.set(Double.NaN));
    }

    @Test
    void malformedJsonRestoresTheDefaultValue() {
        NumberSetting setting = new NumberSetting("speed", "Speed", "Test speed.", 1.0, 0.0, 2.0);
        setting.set(1.5);

        assertFalse(setting.applyJson(new JsonPrimitive(99.0)));
        assertEquals(1.0, setting.value());
    }

    @Test
    void booleanSettingRoundTripsItsValue() {
        BooleanSetting setting = new BooleanSetting("visible", "Visible", "Test visibility.", true);

        assertTrue(setting.applyJson(new JsonPrimitive(false)));
        assertFalse(setting.value());
        assertFalse(setting.applyJson(new JsonPrimitive("false")));
        assertTrue(setting.value());
    }
}
