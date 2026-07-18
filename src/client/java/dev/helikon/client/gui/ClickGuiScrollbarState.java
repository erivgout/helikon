package dev.helikon.client.gui;

import java.util.Optional;
import java.util.OptionalDouble;

/** Minecraft-free drag state and geometry for one ClickGUI vertical scrollbar. */
final class ClickGuiScrollbarState {
    private boolean dragging;
    private double dragOffset;

    OptionalDouble beginDrag(int mouseY, int trackTop, int trackBottom, int contentHeight, double currentScroll) {
        Optional<Thumb> optionalThumb = thumb(trackTop, trackBottom, contentHeight, currentScroll);
        if (optionalThumb.isEmpty()) {
            return OptionalDouble.empty();
        }
        Thumb thumb = optionalThumb.orElseThrow();
        dragging = true;
        dragOffset = mouseY >= thumb.y() && mouseY < thumb.y() + thumb.height()
                ? mouseY - thumb.y() : thumb.height() / 2.0D;
        return OptionalDouble.of(scrollAt(mouseY, trackTop, trackBottom, contentHeight, thumb.height()));
    }

    OptionalDouble dragTo(int mouseY, int trackTop, int trackBottom, int contentHeight) {
        Optional<Thumb> optionalThumb = thumb(trackTop, trackBottom, contentHeight, 0.0D);
        if (!dragging || optionalThumb.isEmpty()) {
            return OptionalDouble.empty();
        }
        return OptionalDouble.of(scrollAt(mouseY, trackTop, trackBottom, contentHeight,
                optionalThumb.orElseThrow().height()));
    }

    boolean isDragging() {
        return dragging;
    }

    void endDrag() {
        dragging = false;
        dragOffset = 0.0D;
    }

    static Optional<Thumb> thumb(int trackTop, int trackBottom, int contentHeight, double scroll) {
        int viewHeight = trackBottom - trackTop;
        if (viewHeight <= 0 || contentHeight <= viewHeight) {
            return Optional.empty();
        }
        int thumbHeight = Math.max(8, viewHeight * viewHeight / contentHeight);
        double maxScroll = contentHeight - viewHeight;
        double boundedScroll = Math.clamp(scroll, 0.0D, maxScroll);
        int thumbY = trackTop + (int) Math.round((viewHeight - thumbHeight) * boundedScroll / maxScroll);
        return Optional.of(new Thumb(thumbY, thumbHeight));
    }

    private double scrollAt(int mouseY, int trackTop, int trackBottom, int contentHeight, int thumbHeight) {
        int viewHeight = trackBottom - trackTop;
        int travel = viewHeight - thumbHeight;
        if (travel <= 0) {
            return 0.0D;
        }
        double thumbY = Math.clamp(mouseY - dragOffset, (double) trackTop,
                (double) (trackBottom - thumbHeight));
        return (thumbY - trackTop) * (contentHeight - viewHeight) / travel;
    }

    record Thumb(int y, int height) {
    }
}
