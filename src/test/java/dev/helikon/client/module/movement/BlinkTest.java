package dev.helikon.client.module.movement;

import com.google.gson.JsonPrimitive;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.Setting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlinkTest {
    @Test
    void bufferDrainsInSendOrderAndEmpties() {
        BlinkBuffer<String> buffer = new BlinkBuffer<>();
        assertTrue(buffer.isEmpty());

        buffer.add("first");
        buffer.add("second");
        buffer.add("third");
        assertEquals(3, buffer.size());
        assertFalse(buffer.isEmpty());

        assertEquals(List.of("first", "second", "third"), buffer.drain());
        assertTrue(buffer.isEmpty());
        assertEquals(List.of(), buffer.drain());
    }

    @Test
    void bufferClearDiscardsWithoutReturning() {
        BlinkBuffer<String> buffer = new BlinkBuffer<>();
        buffer.add("held");
        buffer.clear();
        assertTrue(buffer.isEmpty());
    }

    @Test
    void isDisabledMovementModuleByDefault() {
        Blink module = new Blink();
        assertEquals("blink", module.id());
        assertEquals(ModuleCategory.MOVEMENT, module.category());
        assertFalse(module.isEnabled());
        assertEquals(20, module.maximumHeldPackets());
    }

    @Test
    void holdsOnlyWhileEnabledAndBelowCap() {
        Blink module = new Blink();
        assertFalse(module.shouldHold(0));

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertTrue(module.shouldHold(0));
        assertTrue(module.shouldHold(19));
        assertFalse(module.shouldHold(20));

        registry.setEnabled(module, false);
        assertFalse(module.shouldHold(0));
    }

    @Test
    void releasesWhenDisabledWithHeldPacketsOrWhenCapReached() {
        Blink module = new Blink();

        ModuleRegistry registry = new ModuleRegistry();
        registry.register(module);
        registry.setEnabled(module, true);

        assertFalse(module.shouldRelease(0));
        assertFalse(module.shouldRelease(19));
        assertTrue(module.shouldRelease(20));

        registry.setEnabled(module, false);
        assertFalse(module.shouldRelease(0));
        assertTrue(module.shouldRelease(1));
    }

    @Test
    void rejectsNegativeHeldCounts() {
        Blink module = new Blink();
        assertThrows(IllegalArgumentException.class, () -> module.shouldHold(-1));
        assertThrows(IllegalArgumentException.class, () -> module.shouldRelease(-1));
    }

    @Test
    void capSettingIsBoundedAndRecoversFromInvalidJson() {
        Blink module = new Blink();
        IntegerSetting cap = (IntegerSetting) module.settings().stream()
                .filter(setting -> setting.id().equals("max_held_packets"))
                .findFirst()
                .orElseThrow();
        assertEquals(1, cap.minimum());
        assertEquals(200, cap.maximum());

        cap.set(50);
        assertEquals(50, module.maximumHeldPackets());

        assertThrows(IllegalArgumentException.class, () -> cap.set(0));
        assertThrows(IllegalArgumentException.class, () -> cap.set(201));

        Setting<?> setting = cap;
        assertFalse(setting.applyJson(new JsonPrimitive(9999)));
        assertEquals(20, module.maximumHeldPackets());
    }
}
