package dev.helikon.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/** Parses compact, bounded local ChatSuffix list settings without Minecraft dependencies. */
public final class SuffixOptions {
    private SuffixOptions() {
    }

    public static Optional<String> select(String defaultSuffix, String perServerEntries, String randomEntries,
                                          String serverAddress, int randomIndex) {
        Map<String, String> perServer = parsePerServer(perServerEntries);
        if (serverAddress != null) {
            String selected = perServer.get(serverAddress.trim().toLowerCase(Locale.ROOT));
            if (selected != null) {
                return Optional.of(selected);
            }
        }
        List<String> random = parseList(randomEntries);
        if (!random.isEmpty()) {
            return Optional.of(random.get(Math.floorMod(randomIndex, random.size())));
        }
        String normalizedDefault = defaultSuffix == null ? "" : defaultSuffix.trim();
        return normalizedDefault.isEmpty() ? Optional.empty() : Optional.of(normalizedDefault);
    }

    static Map<String, String> parsePerServer(String entries) {
        Map<String, String> parsed = new TreeMap<>();
        if (entries == null || entries.isBlank()) {
            return Map.of();
        }
        for (String entry : entries.split(";")) {
            int separator = entry.indexOf('=');
            if (separator <= 0) {
                continue;
            }
            String server = entry.substring(0, separator).trim().toLowerCase(Locale.ROOT);
            String suffix = entry.substring(separator + 1).trim();
            if (!server.isEmpty() && !suffix.isEmpty()) {
                parsed.put(server, suffix);
            }
        }
        return Map.copyOf(parsed);
    }

    static List<String> parseList(String entries) {
        if (entries == null || entries.isBlank()) {
            return List.of();
        }
        List<String> parsed = new ArrayList<>();
        for (String entry : entries.split(",")) {
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        return List.copyOf(parsed);
    }
}
