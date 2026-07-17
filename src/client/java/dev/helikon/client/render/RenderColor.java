package dev.helikon.client.render;

/** ARGB color helpers kept outside render adapters for deterministic local transparency behavior. */
public final class RenderColor {
    private RenderColor() {
    }

    public static int withAlpha(int color, double multiplier) {
        if (!Double.isFinite(multiplier)) {
            throw new IllegalArgumentException("alpha multiplier must be finite");
        }
        int alpha = (color >>> 24) & 0xFF;
        int adjusted = Math.clamp((int) Math.round(alpha * multiplier), 0, 255);
        return adjusted << 24 | color & 0x00FFFFFF;
    }
}
