package dev.helikon.client.chat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/** Bounded keyword and regex matching with conservative regex safety limits. */
public final class TextMatchRules {
    private static final int MAX_PATTERN_LENGTH = 96;
    private static final int MAX_PATTERN_COUNT = 8;

    private TextMatchRules() {
    }

    public static boolean containsAny(String text, String entries, boolean caseSensitive) {
        if (entries == null || entries.isBlank()) {
            return false;
        }
        String subject = normalize(text, caseSensitive);
        for (String entry : entries.split(",")) {
            String token = normalize(entry.trim(), caseSensitive);
            if (!token.isEmpty() && subject.contains(token)) {
                return true;
            }
        }
        return false;
    }

    public static boolean matchesRegex(String text, String entries, boolean caseSensitive) {
        for (Pattern pattern : compile(entries, caseSensitive)) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }

    public static List<Pattern> compile(String entries, boolean caseSensitive) {
        if (entries == null || entries.isBlank()) {
            return List.of();
        }
        List<Pattern> patterns = new ArrayList<>();
        for (String entry : entries.split(";")) {
            String expression = entry.trim();
            if (expression.isEmpty()) {
                continue;
            }
            if (patterns.size() >= MAX_PATTERN_COUNT || !isSafeRegex(expression)) {
                return List.of();
            }
            try {
                patterns.add(Pattern.compile(expression, caseSensitive ? 0 : Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            } catch (PatternSyntaxException exception) {
                return List.of();
            }
        }
        return List.copyOf(patterns);
    }

    static boolean isSafeRegex(String expression) {
        return expression.length() <= MAX_PATTERN_LENGTH
                && !expression.contains("\\1")
                && !expression.contains("(?=")
                && !expression.contains("(?!")
                && !expression.contains("(?<=")
                && !expression.contains("(?<!")
                // Quantifying a group permits catastrophic alternatives such as
                // (a|aa)+ and nested forms such as (a+)+. Rejecting all such
                // groups keeps this local incoming-message filter predictable.
                && !expression.matches(".*\\([^)]*\\)[+*{].*");
    }

    private static String normalize(String value, boolean caseSensitive) {
        String nonNull = value == null ? "" : value;
        return caseSensitive ? nonNull : nonNull.toLowerCase(Locale.ROOT);
    }
}
