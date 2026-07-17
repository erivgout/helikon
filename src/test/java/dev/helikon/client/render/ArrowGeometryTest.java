package dev.helikon.client.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrowGeometryTest {
    @Test
    void buildsARightPointingTriangleReachingTheTip() {
        List<ArrowGeometry.Span> spans = ArrowGeometry.build(100.0D, 100.0D, 20.0D, 10.0D, 4.0D, 1.0D, 0.0D);
        assertFalse(spans.isEmpty());

        int maxEndX = spans.stream().mapToInt(ArrowGeometry.Span::xEnd).max().orElseThrow();
        int minStartX = spans.stream().mapToInt(ArrowGeometry.Span::xStart).min().orElseThrow();
        // The tip sits at centerX + ringRadius + length = 130; the base sits at 120.
        assertEquals(130, maxEndX, 1);
        assertEquals(120, minStartX, 1);

        for (ArrowGeometry.Span span : spans) {
            assertTrue(span.xEnd() > span.xStart(), "every span must have positive width");
        }
    }

    @Test
    void triangleIsVerticallySymmetricAboutTheCenterForAHorizontalArrow() {
        List<ArrowGeometry.Span> spans = ArrowGeometry.build(0.0D, 0.0D, 0.0D, 10.0D, 6.0D, 1.0D, 0.0D);
        int minRow = spans.stream().mapToInt(ArrowGeometry.Span::y).min().orElseThrow();
        int maxRow = spans.stream().mapToInt(ArrowGeometry.Span::y).max().orElseThrow();
        // Base half-width is 6, centered on row 0, so rows span roughly [-6, 6).
        assertEquals(-(maxRow + 1), minRow, 1);
    }

    @Test
    void rejectsNonPositiveSizeAndZeroDirection() {
        assertThrows(IllegalArgumentException.class,
                () -> ArrowGeometry.build(0.0D, 0.0D, 0.0D, 0.0D, 4.0D, 1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> ArrowGeometry.build(0.0D, 0.0D, 0.0D, 10.0D, 4.0D, 0.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> ArrowGeometry.build(Double.NaN, 0.0D, 0.0D, 10.0D, 4.0D, 1.0D, 0.0D));
    }
}
