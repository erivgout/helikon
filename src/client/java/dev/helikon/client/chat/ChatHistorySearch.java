package dev.helikon.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Bounded case-insensitive local chat-history search without Minecraft dependencies. */
public final class ChatHistorySearch {
    private ChatHistorySearch() {
    }

    public static List<String> find(List<String> newestFirst, String query, int maximumResults) {
        Objects.requireNonNull(newestFirst, "newestFirst");
        String needle = requireQuery(query);
        int limit = Math.clamp(maximumResults, 1, 20);
        List<String> matches = new ArrayList<>();
        for (String line : newestFirst) {
            if (line != null && line.toLowerCase(Locale.ROOT).contains(needle)) {
                matches.add(line);
                if (matches.size() == limit) {
                    break;
                }
            }
        }
        return List.copyOf(matches);
    }

    public static String requireQuery(String query) {
        if (query == null) {
            throw new IllegalArgumentException("Search text is required.");
        }
        String checked = query.trim();
        if (checked.isEmpty() || checked.length() > 80 || checked.indexOf('\n') >= 0 || checked.indexOf('\r') >= 0) {
            throw new IllegalArgumentException("Search text must be 1 through 80 single-line characters.");
        }
        return checked.toLowerCase(Locale.ROOT);
    }
}
