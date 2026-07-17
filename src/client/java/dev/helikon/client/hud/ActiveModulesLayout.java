package dev.helikon.client.hud;

import java.util.Objects;

/** Validated, Minecraft-free presentation and placement state for Active Modules. */
public final class ActiveModulesLayout {
    public static final float MIN_SCALE = 0.5F;
    public static final float MAX_SCALE = 3.0F;
    public static final int MIN_PADDING = 0;
    public static final int MAX_PADDING = 12;

    public enum Sort {
        REGISTRY,
        ALPHABETICAL,
        WIDTH
    }

    public enum Alignment {
        LEFT,
        RIGHT
    }

    public enum ColorMode {
        ACCENT,
        RAINBOW
    }

    private boolean enabled = true;
    private int x = HudLayout.DEFAULT_ACTIVE_MODULES_X;
    private int y = HudLayout.DEFAULT_ACTIVE_MODULES_Y;
    private float scale = 1.0F;
    private int padding = ActiveModulesHud.PADDING;
    private boolean background = true;
    private boolean textShadow = true;
    private Sort sort = Sort.REGISTRY;
    private Alignment alignment = Alignment.LEFT;
    private ColorMode colorMode = ColorMode.ACCENT;
    private boolean animations = true;

    public boolean enabled() { return enabled; }
    public int x() { return x; }
    public int y() { return y; }
    public float scale() { return scale; }
    public int padding() { return padding; }
    public boolean background() { return background; }
    public boolean textShadow() { return textShadow; }
    public Sort sort() { return sort; }
    public Alignment alignment() { return alignment; }
    public ColorMode colorMode() { return colorMode; }
    public boolean animations() { return animations; }

    public void setEnabled(boolean value) { enabled = value; }

    public boolean setPosition(int nextX, int nextY) {
        if (!HudLayout.isValidCoordinate(nextX) || !HudLayout.isValidCoordinate(nextY)) {
            return false;
        }
        x = nextX;
        y = nextY;
        return true;
    }

    public boolean setScale(float value) {
        if (!Float.isFinite(value) || value < MIN_SCALE || value > MAX_SCALE) {
            return false;
        }
        scale = value;
        return true;
    }

    public boolean setPadding(int value) {
        if (value < MIN_PADDING || value > MAX_PADDING) {
            return false;
        }
        padding = value;
        return true;
    }

    public void setBackground(boolean value) { background = value; }
    public void setTextShadow(boolean value) { textShadow = value; }
    public void setSort(Sort value) { sort = Objects.requireNonNull(value, "value"); }
    public void setAlignment(Alignment value) { alignment = Objects.requireNonNull(value, "value"); }
    public void setColorMode(ColorMode value) { colorMode = Objects.requireNonNull(value, "value"); }
    public void setAnimations(boolean value) { animations = value; }

    public void reset() {
        enabled = true;
        x = HudLayout.DEFAULT_ACTIVE_MODULES_X;
        y = HudLayout.DEFAULT_ACTIVE_MODULES_Y;
        scale = 1.0F;
        padding = ActiveModulesHud.PADDING;
        background = true;
        textShadow = true;
        sort = Sort.REGISTRY;
        alignment = Alignment.LEFT;
        colorMode = ColorMode.ACCENT;
        animations = true;
    }
}
