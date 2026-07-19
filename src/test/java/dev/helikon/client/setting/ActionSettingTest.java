package dev.helikon.client.setting;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActionSettingTest {
    @Test
    void runsOnlyWhenExplicitlyInvokedAndPersistsNoState() {
        AtomicInteger calls = new AtomicInteger();
        ActionSetting setting = new ActionSetting("stop", "Stop", "Stops work.", calls::incrementAndGet);

        assertFalse(setting.value());
        assertTrue(setting.applyJson(new com.google.gson.JsonPrimitive(true)));
        assertEquals(0, calls.get());

        setting.run();

        assertEquals(1, calls.get());
        assertFalse(setting.toJson().getAsBoolean());
    }
}
