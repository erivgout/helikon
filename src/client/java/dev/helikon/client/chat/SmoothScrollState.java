package dev.helikon.client.chat;

/** Small bounded easing state for local multi-line chat scroll requests. */
public final class SmoothScrollState {
    private int pendingLines;

    public void request(int lines) {
        pendingLines = Math.clamp(pendingLines + lines, -200, 200);
    }

    public int nextStep() {
        if (pendingLines == 0) {
            return 0;
        }
        int direction = Integer.signum(pendingLines);
        int step = Math.max(1, (int) Math.ceil(Math.abs(pendingLines) * 0.35D));
        step = Math.min(step, Math.abs(pendingLines));
        pendingLines -= direction * step;
        return direction * step;
    }

    public int pendingLines() {
        return pendingLines;
    }

    public void clear() {
        pendingLines = 0;
    }
}
