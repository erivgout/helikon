package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Explicit opt-in gate for the public GitHub release integration. */
public final class UpdateChecker extends Module {
    public UpdateChecker() {
        super("update_checker", "Update Checker",
                "Checks Helikon's public GitHub releases once per client session and reports newer versions.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }
}
