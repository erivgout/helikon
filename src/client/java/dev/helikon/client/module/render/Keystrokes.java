package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.hud.ClickRateTracker;

/** Configures a compact, read-only HUD view of the player's current input state. */
public final class Keystrokes extends Module {
    private final BooleanSetting showMouseButtons;
    private final BooleanSetting showJump;
    private final BooleanSetting showCps;
    private final ClickRateTracker clickRates = new ClickRateTracker();

    public Keystrokes() {
        super("keystrokes", "Keystrokes", "Shows movement, mouse, and jump key states in the HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        showMouseButtons = addSetting(new BooleanSetting("show_mouse_buttons", "Show mouse buttons",
                "Include the attack and use buttons.", true));
        showJump = addSetting(new BooleanSetting("show_jump", "Show jump",
                "Include the jump key beneath the movement keys.", true));
        showCps = addSetting(new BooleanSetting("show_cps", "Show CPS",
                "Show rolling left and right clicks per second inside the mouse buttons.", true,
                showMouseButtons::value));
    }

    public boolean showMouseButtons() {
        return showMouseButtons.value();
    }

    public boolean showJump() {
        return showJump.value();
    }

    public boolean showCps() {
        return showMouseButtons() && showCps.value();
    }

    public void recordClick(ClickRateTracker.Button button, long nowNanos) {
        if (isEnabled()) {
            clickRates.record(button, nowNanos);
        }
    }

    public int clicksPerSecond(ClickRateTracker.Button button, long nowNanos) {
        return clickRates.clicksPerSecond(button, nowNanos);
    }

    @Override
    protected void onDisable() {
        clickRates.clear();
    }
}
