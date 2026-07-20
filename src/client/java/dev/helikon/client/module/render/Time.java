package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;

/** Configures a local clock HUD with real-world or current-world time. */
public final class Time extends Module {
    public enum Source {
        LOCAL,
        WORLD
    }

    private final EnumSetting<Source> source;
    private final BooleanSetting twentyFourHour;
    private final BooleanSetting showSeconds;

    public Time() {
        super("time", "Time", "Shows local real-world or Minecraft world time in the HUD.",
                ModuleCategory.RENDER, false, Keybind.unbound());
        source = addSetting(new EnumSetting<>("source", "Source",
                "Choose the computer's local clock or the current Minecraft world time.",
                Source.class, Source.LOCAL));
        twentyFourHour = addSetting(new BooleanSetting("twenty_four_hour", "24-hour clock",
                "Use 24-hour time instead of AM/PM.", true));
        showSeconds = addSetting(new BooleanSetting("show_seconds", "Show seconds",
                "Include seconds when using the local clock.", false,
                () -> source.value() == Source.LOCAL));
    }

    public Source source() {
        return source.value();
    }

    public boolean twentyFourHour() {
        return twentyFourHour.value();
    }

    public boolean showSeconds() {
        return showSeconds.value();
    }
}
