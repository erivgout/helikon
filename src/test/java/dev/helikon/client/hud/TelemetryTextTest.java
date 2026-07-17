package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TelemetryTextTest {
    @Test
    void formatsDirectionsAndClockFromLocalValues() {
        assertEquals("Direction South", TelemetryText.direction(0.0F));
        assertEquals("Direction West", TelemetryText.direction(90.0F));
        assertEquals("Clock 06:00", TelemetryText.clock(0L));
        assertEquals("Clock 00:00", TelemetryText.clock(18_000L));
    }

    @Test
    void keepsTelemetryTextBoundedAndReadable() {
        assertEquals("FPS 0", TelemetryText.fps(-1));
        assertEquals("Ping --", TelemetryText.ping(-1));
        assertEquals("TPS 20.0 (local)", TelemetryText.tps(24.0D));
        assertEquals("Flower Forest", TelemetryText.titleCaseIdentifier("minecraft:flower_forest"));
        assertEquals("Armor 50%", TelemetryText.durability("Armor", 5, 10));
    }
}
