package dev.helikon.client.render;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;

/** Validated, Minecraft-free local BlockESP color overrides keyed by block identifier. */
public final class BlockColorMap {
    private BlockColorMap() {
    }

    public static Map<String, Integer> parse(String value) {
        Map<String, Integer> colors = new LinkedHashMap<>();
        if (value == null) return Map.of();
        for (String entry : value.split(";")) {
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator != entry.lastIndexOf('=')) continue;
            String id = entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            if (!BlockIdList.parse(id).contains(id)) continue;
            parseColor(entry.substring(separator + 1).trim()).ifPresent(color -> colors.put(id, color));
        }
        return Map.copyOf(colors);
    }

    public static OptionalInt parseColor(String value) {
        if (value == null || !value.matches("#[0-9A-Fa-f]{6}|#[0-9A-Fa-f]{8}")) return OptionalInt.empty();
        long parsed = Long.parseLong(value.substring(1), 16);
        return OptionalInt.of(value.length() == 7 ? (int) (0xFF000000L | parsed) : (int) parsed);
    }
}
