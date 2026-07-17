package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minecraft-free local player-name replacement policy for on-screen text. */
public final class NameProtect extends Module {
    private final StringSetting alias;
    private final BooleanSetting caseSensitive;
    private final BooleanSetting wholeNameOnly;
    private final IntegerSetting maximumReplacements;

    public NameProtect() {
        super("name_protect", "NameProtect", "Replaces the local player name in rendered text for privacy.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        alias = addSetting(new StringSetting("alias", "Alias",
                "Bounded local replacement; aliases containing the real name fall back to asterisks.",
                "Protected", 32, false));
        caseSensitive = addSetting(new BooleanSetting("case_sensitive", "Case sensitive",
                "Replace only capitalization that exactly matches the profile name.", false));
        wholeNameOnly = addSetting(new BooleanSetting("whole_name_only", "Whole name only",
                "Avoid replacing the same characters inside a longer username-like token.", true));
        maximumReplacements = addSetting(new IntegerSetting("maximum_replacements", "Maximum replacements",
                "Hard cap for replacements in one rendered text value.", 16, 1, 64));
    }

    /** Replaces bounded matching ranges while leaving the source string untouched when disabled. */
    public String protect(String text, String localPlayerName) {
        List<ReplacementRange> ranges = replacementRanges(text, localPlayerName);
        if (ranges.isEmpty()) {
            return text;
        }
        String replacement = aliasFor(localPlayerName);
        StringBuilder protectedText = new StringBuilder(Math.max(16,
                text.length() + ranges.size() * (replacement.length() - localPlayerName.length())));
        int cursor = 0;
        for (ReplacementRange range : ranges) {
            protectedText.append(text, cursor, range.start());
            protectedText.append(replacement);
            cursor = range.end();
        }
        return protectedText.append(text, cursor, text.length()).toString();
    }

    /** Returns non-overlapping UTF-16 source ranges for style-preserving render adapters. */
    public List<ReplacementRange> replacementRanges(String text, String localPlayerName) {
        if (!isEnabled() || text == null || text.isEmpty()
                || localPlayerName == null || localPlayerName.isBlank()) {
            return List.of();
        }
        String name = localPlayerName.trim();
        if (name.isEmpty() || name.length() > text.length()) {
            return List.of();
        }
        List<ReplacementRange> ranges = new ArrayList<>();
        int lastStart = text.length() - name.length();
        for (int start = 0; start <= lastStart && ranges.size() < maximumReplacements.value(); start++) {
            if (!text.regionMatches(!caseSensitive.value(), start, name, 0, name.length())) {
                continue;
            }
            int end = start + name.length();
            if (wholeNameOnly.value() && (!isBoundary(text, start - 1) || !isBoundary(text, end))) {
                continue;
            }
            ranges.add(new ReplacementRange(start, end));
            start = end - 1;
        }
        return List.copyOf(ranges);
    }

    /** Resolves a bounded alias that cannot accidentally contain the protected name. */
    public String aliasFor(String localPlayerName) {
        String configured = alias.value();
        if (localPlayerName == null || localPlayerName.isBlank()
                || !configured.toLowerCase(Locale.ROOT)
                .contains(localPlayerName.trim().toLowerCase(Locale.ROOT))) {
            return configured;
        }
        return "*".repeat(Math.max(3, Math.min(16, localPlayerName.trim().length())));
    }

    private static boolean isBoundary(String text, int index) {
        return index < 0 || index >= text.length() || !isNameCharacter(text.charAt(index));
    }

    private static boolean isNameCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    public record ReplacementRange(int start, int end) {
        public ReplacementRange {
            if (start < 0 || end <= start) {
                throw new IllegalArgumentException("Replacement range must be non-empty and ordered");
            }
        }
    }
}
