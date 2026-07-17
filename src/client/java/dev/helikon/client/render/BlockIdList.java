package dev.helikon.client.render;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/** Parses a bounded semicolon-delimited local block-ID list without registry access. */
public final class BlockIdList {
    public static final int MAXIMUM_IDS = 32;
    private static final String IDENTIFIER_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+";

    private BlockIdList() {
    }

    public static Set<String> parse(String configuredIds) {
        if (configuredIds == null) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (String value : configuredIds.split(";")) {
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.matches(IDENTIFIER_PATTERN)) {
                ids.add(normalized);
                if (ids.size() == MAXIMUM_IDS) {
                    break;
                }
            }
        }
        return Set.copyOf(ids);
    }
}
