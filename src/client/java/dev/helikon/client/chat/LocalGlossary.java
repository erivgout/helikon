package dev.helikon.client.chat;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** Parses a small bounded `source=translation` glossary without filesystem or Minecraft dependencies. */
public final class LocalGlossary {
    private static final int MAXIMUM_ENTRIES = 64;

    private LocalGlossary() {
    }

    public static Optional<String> translate(String text, String configuration) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(parse(configuration).get(normalize(text)));
    }

    public static Map<String, String> parse(String configuration) {
        Map<String, String> entries = new LinkedHashMap<>();
        if (configuration == null || configuration.isBlank()) {
            return Map.of();
        }
        for (String entry : configuration.split(";", -1)) {
            if (entry.isBlank()) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator != entry.lastIndexOf('=')) {
                continue;
            }
            String source = entry.substring(0, separator).trim();
            String translated = entry.substring(separator + 1).trim();
            if (!isSafeText(source) || !isSafeText(translated) || entries.size() >= MAXIMUM_ENTRIES) {
                continue;
            }
            entries.putIfAbsent(normalize(source), translated);
        }
        return Map.copyOf(entries);
    }

    private static boolean isSafeText(String text) {
        return !text.isEmpty() && text.length() <= 128 && text.indexOf('\r') < 0 && text.indexOf('\n') < 0;
    }

    private static String normalize(String text) {
        return text.trim().toLowerCase(Locale.ROOT);
    }
}
