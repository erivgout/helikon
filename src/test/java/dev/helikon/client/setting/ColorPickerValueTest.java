package dev.helikon.client.setting;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ColorPickerValueTest {
    @Test
    void replacesIndividualArgbChannelsWithoutTouchingTheOthers() {
        int color = 0x80402010;

        assertEquals(0xFF402010, ColorPickerValue.withChannel(color, 0, 255));
        assertEquals(0x80AA2010, ColorPickerValue.withChannel(color, 1, 0xAA));
        assertEquals(0x8040BB10, ColorPickerValue.withChannel(color, 2, 0xBB));
        assertEquals(0x804020CC, ColorPickerValue.withChannel(color, 3, 0xCC));
    }

    @Test
    void pickerCoordinatesCoverBothChannelEndpoints() {
        assertEquals(0, ColorPickerValue.channelAt(-30, 10, 100));
        assertEquals(0, ColorPickerValue.channelAt(10, 10, 100));
        assertEquals(255, ColorPickerValue.channelAt(109, 10, 100));
        assertEquals(255, ColorPickerValue.channelAt(140, 10, 100));
        assertThrows(IllegalArgumentException.class, () -> ColorPickerValue.channelAt(0, 0, 1));
    }
}
