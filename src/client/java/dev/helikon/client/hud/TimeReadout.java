package dev.helikon.client.hud;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Minecraft-free formatting for local and Minecraft-world clocks. */
public final class TimeReadout {
    private static final DateTimeFormatter TWENTY_FOUR_HOUR = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter TWENTY_FOUR_HOUR_SECONDS =
            DateTimeFormatter.ofPattern("HH:mm:ss", Locale.ROOT);
    private static final DateTimeFormatter TWELVE_HOUR = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
    private static final DateTimeFormatter TWELVE_HOUR_SECONDS =
            DateTimeFormatter.ofPattern("h:mm:ss a", Locale.US);

    private TimeReadout() {
    }

    public static String local(LocalTime time, boolean twentyFourHour, boolean showSeconds) {
        Objects.requireNonNull(time, "time");
        DateTimeFormatter formatter = twentyFourHour
                ? (showSeconds ? TWENTY_FOUR_HOUR_SECONDS : TWENTY_FOUR_HOUR)
                : (showSeconds ? TWELVE_HOUR_SECONDS : TWELVE_HOUR);
        return "Time " + formatter.format(time);
    }

    public static String world(long worldTime, boolean twentyFourHour) {
        long normalized = Math.floorMod(worldTime + 6_000L, 24_000L);
        int minutes = (int) (normalized * 1_440L / 24_000L);
        return local(LocalTime.of(minutes / 60, minutes % 60), twentyFourHour, false);
    }
}
