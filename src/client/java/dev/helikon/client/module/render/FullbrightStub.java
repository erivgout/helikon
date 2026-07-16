package dev.helikon.client.module.render;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * A registration and settings smoke-test module. It intentionally does not
 * change gamma or apply any gameplay behavior until the render milestone.
 */
public final class FullbrightStub extends Module {
    private final BooleanSetting enabledMode;
    private final NumberSetting brightness;

    public FullbrightStub() {
        super(
                "fullbright_stub",
                "Fullbright (Stub)",
                "Bootstrap-only placeholder. It does not modify client brightness.",
                ModuleCategory.RENDER,
                false,
                Keybind.unbound()
        );
        enabledMode = addSetting(new BooleanSetting(
                "use_gamma",
                "Gamma mode",
                "Reserved for the real Fullbright implementation.",
                true
        ));
        brightness = addSetting(new NumberSetting(
                "brightness",
                "Brightness",
                "Reserved for the real Fullbright implementation.",
                1.0,
                0.0,
                10.0
        ));
    }

    public BooleanSetting enabledMode() {
        return enabledMode;
    }

    public NumberSetting brightness() {
        return brightness;
    }
}
