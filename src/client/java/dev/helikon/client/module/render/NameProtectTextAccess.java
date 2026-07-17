package dev.helikon.client.module.render;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Thin 26.2 font adapter that preserves styles while applying NameProtect's pure range policy. */
public final class NameProtectTextAccess {
    private static final int MAXIMUM_INPUT_CHARACTERS = 4_096;
    private static volatile NameProtect nameProtect;

    private NameProtectTextAccess() {
    }

    public static void install(NameProtect module) {
        nameProtect = Objects.requireNonNull(module, "module");
    }

    public static String protect(String text) {
        NameContext context = context();
        return context == null || text == null || text.length() > MAXIMUM_INPUT_CHARACTERS
                ? text
                : context.module().protect(text, context.localName());
    }

    public static FormattedCharSequence protect(FormattedCharSequence source) {
        NameContext context = context();
        if (context == null || source == null) {
            return source;
        }
        List<StyledCodePoint> codePoints = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean completed = source.accept((ignoredIndex, style, codePoint) -> {
            if (text.length() + Character.charCount(codePoint) > MAXIMUM_INPUT_CHARACTERS) {
                return false;
            }
            int start = text.length();
            text.appendCodePoint(codePoint);
            codePoints.add(new StyledCodePoint(codePoint, style, start, text.length()));
            return true;
        });
        if (!completed) {
            return source;
        }
        List<NameProtect.ReplacementRange> ranges =
                context.module().replacementRanges(text.toString(), context.localName());
        if (ranges.isEmpty()) {
            return source;
        }
        String alias = context.module().aliasFor(context.localName());
        return sink -> emitProtected(codePoints, ranges, alias, sink);
    }

    public static FormattedText protect(FormattedText source) {
        NameContext context = context();
        if (context == null || source == null) {
            return source;
        }
        List<StyledSegment> segments = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        boolean[] exceeded = {false};
        source.visit((style, value) -> {
            if (text.length() + value.length() > MAXIMUM_INPUT_CHARACTERS) {
                exceeded[0] = true;
                return FormattedText.STOP_ITERATION;
            }
            int start = text.length();
            text.append(value);
            segments.add(new StyledSegment(start, text.length(), value, style));
            return Optional.empty();
        }, Style.EMPTY);
        if (exceeded[0]) {
            return source;
        }
        List<NameProtect.ReplacementRange> ranges =
                context.module().replacementRanges(text.toString(), context.localName());
        if (ranges.isEmpty()) {
            return source;
        }
        List<FormattedText> pieces = new ArrayList<>();
        int cursor = 0;
        for (NameProtect.ReplacementRange range : ranges) {
            appendOriginal(pieces, segments, cursor, range.start());
            pieces.add(FormattedText.of(context.module().aliasFor(context.localName()),
                    styleAt(segments, range.start())));
            cursor = range.end();
        }
        appendOriginal(pieces, segments, cursor, text.length());
        return FormattedText.composite(pieces);
    }

    private static boolean emitProtected(
            List<StyledCodePoint> codePoints,
            List<NameProtect.ReplacementRange> ranges,
            String alias,
            net.minecraft.util.FormattedCharSink sink
    ) {
        int rangeIndex = 0;
        int outputIndex = 0;
        for (int pointIndex = 0; pointIndex < codePoints.size();) {
            StyledCodePoint point = codePoints.get(pointIndex);
            NameProtect.ReplacementRange range = rangeIndex < ranges.size() ? ranges.get(rangeIndex) : null;
            if (range != null && point.start() == range.start()) {
                for (int aliasIndex = 0; aliasIndex < alias.length();) {
                    int codePoint = alias.codePointAt(aliasIndex);
                    if (!sink.accept(outputIndex, point.style(), codePoint)) {
                        return false;
                    }
                    int width = Character.charCount(codePoint);
                    aliasIndex += width;
                    outputIndex += width;
                }
                while (pointIndex < codePoints.size() && codePoints.get(pointIndex).end() <= range.end()) {
                    pointIndex++;
                }
                rangeIndex++;
                continue;
            }
            if (!sink.accept(outputIndex, point.style(), point.codePoint())) {
                return false;
            }
            outputIndex += Character.charCount(point.codePoint());
            pointIndex++;
        }
        return true;
    }

    private static void appendOriginal(
            List<FormattedText> output,
            List<StyledSegment> segments,
            int start,
            int end
    ) {
        if (start >= end) {
            return;
        }
        for (StyledSegment segment : segments) {
            int intersectionStart = Math.max(start, segment.start());
            int intersectionEnd = Math.min(end, segment.end());
            if (intersectionStart < intersectionEnd) {
                int localStart = intersectionStart - segment.start();
                int localEnd = intersectionEnd - segment.start();
                output.add(FormattedText.of(segment.text().substring(localStart, localEnd), segment.style()));
            }
        }
    }

    private static Style styleAt(List<StyledSegment> segments, int index) {
        for (StyledSegment segment : segments) {
            if (index >= segment.start() && index < segment.end()) {
                return segment.style();
            }
        }
        return Style.EMPTY;
    }

    private static NameContext context() {
        NameProtect module = nameProtect;
        Minecraft client = Minecraft.getInstance();
        if (module == null || !module.isEnabled() || client == null || client.player == null) {
            return null;
        }
        String localName = client.player.getGameProfile().name();
        return localName == null || localName.isBlank() ? null : new NameContext(module, localName);
    }

    private record NameContext(NameProtect module, String localName) {
    }

    private record StyledCodePoint(int codePoint, Style style, int start, int end) {
    }

    private record StyledSegment(int start, int end, String text, Style style) {
    }
}
