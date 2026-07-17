package dev.helikon.client.chat;

import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.chat.ChatTimestamps;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class ChatTimestampFormatTest {
    private static final Instant SESSION_START = Instant.parse("2026-07-16T12:00:00Z");
    private static final Instant MESSAGE_TIME = Instant.parse("2026-07-16T13:05:09Z");

    @Test
    void formatsTwentyFourHourAndTwelveHourClockLabels() {
        assertEquals("[13:05] ", ChatTimestampFormat.format(MESSAGE_TIME, SESSION_START, ZoneId.of("UTC"),
                true, false, true, false));
        assertEquals("01:05:09 PM ", ChatTimestampFormat.format(MESSAGE_TIME, SESSION_START, ZoneId.of("UTC"),
                false, true, false, false));
    }

    @Test
    void formatsRelativeSessionTimeWithoutNegativeValues() {
        assertEquals("[+1h] ", ChatTimestampFormat.format(MESSAGE_TIME, SESSION_START, ZoneId.of("UTC"),
                true, false, true, true));
        assertEquals("+0s ", ChatTimestampFormat.format(SESSION_START.minusSeconds(5), SESSION_START, ZoneId.of("UTC"),
                true, false, false, true));
    }

    @Test
    void stylesOnlyTheTimestampSiblingAndPreservesTheOriginalComponent() {
        ChatTimestamps timestamps = new ChatTimestamps();
        ModuleRegistry registry = new ModuleRegistry();
        registry.register(timestamps);
        registry.setEnabled(timestamps, true);
        ChatDisplayAccess.install(timestamps);
        Component original = Component.literal("hello");

        Component decorated = ChatDisplayAccess.decorateTimestamp(original);

        assertNull(decorated.getStyle().getColor());
        assertEquals(2, decorated.getSiblings().size());
        assertEquals(0x808080, decorated.getSiblings().getFirst().getStyle().getColor().getValue());
        assertSame(original, decorated.getSiblings().get(1));
    }
}
