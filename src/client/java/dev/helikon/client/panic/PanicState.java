package dev.helikon.client.panic;

/** Session-only panic presentation state; persisted HUD preferences stay untouched. */
public final class PanicState {
    private boolean customHudHidden;

    public boolean customHudHidden() {
        return customHudHidden;
    }

    public void hideCustomHud() {
        customHudHidden = true;
    }

    /** Restores normal HUD rendering without re-enabling any module. */
    public void restoreCustomHud() {
        customHudHidden = false;
    }
}
