package dev.helikon.client.hud;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeReadoutTest {
    @Test
    void formatsLocalTimeInTwelveAndTwentyFourHourModes() {
        LocalTime time = LocalTime.of(21, 7, 5);
        assertEquals("Time 21:07", TimeReadout.local(time, true, false));
        assertEquals("Time 21:07:05", TimeReadout.local(time, true, true));
        assertEquals("Time 9:07 PM", TimeReadout.local(time, false, false));
    }

    @Test
    void convertsMinecraftWorldTime() {
        assertEquals("Time 06:00", TimeReadout.world(0L, true));
        assertEquals("Time 12:00 AM", TimeReadout.world(18_000L, false));
    }
}
