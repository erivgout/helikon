package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Locally hides falling rain and/or snow precipitation, including during thunderstorms. */
public final class NoWeather extends Module {
    private final BooleanSetting rain;
    private final BooleanSetting snow;

    public NoWeather() {
        super("no_weather", "NoWeather",
                "Hides local rain and snow precipitation rendering, including during thunderstorms.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        rain = addSetting(new BooleanSetting("rain", "Rain", "Hide local falling-rain precipitation.", true));
        snow = addSetting(new BooleanSetting("snow", "Snow", "Hide local falling-snow precipitation.", true));
    }

    /** Whether the local rain precipitation columns should be cleared before rendering. */
    public boolean hidesRain() {
        return isEnabled() && rain.value();
    }

    /** Whether the local snow precipitation columns should be cleared before rendering. */
    public boolean hidesSnow() {
        return isEnabled() && snow.value();
    }
}
