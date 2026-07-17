package dev.helikon.client.module.world;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.StringListSetting;

import java.util.ArrayList;
import java.util.List;

/** Supplies four bounded, protocol-safe lines to newly opened sign editors. */
public final class AutoSign extends Module {
    private final StringListSetting lines;

    public AutoSign() {
        super("auto_sign", "AutoSign", "Fills and submits newly opened sign editors with configured text.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        lines = addSetting(new StringListSetting("lines", "Lines",
                "Up to four sign lines; blank lines are allowed.", List.of("Helikon"), 4, 90, true));
    }

    public List<String> fourLines() {
        List<String> result = new ArrayList<>(List.of("", "", "", ""));
        for (int index = 0; index < lines.value().size() && index < 4; index++) {
            result.set(index, lines.value().get(index));
        }
        return List.copyOf(result);
    }
}
