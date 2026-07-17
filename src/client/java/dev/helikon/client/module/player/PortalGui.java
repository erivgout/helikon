package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Keeps the current local screen open when a portal transition begins. */
public final class PortalGui extends Module {
    public PortalGui() {
        super("portal_gui", "PortalGUI", "Keeps interfaces open and usable while inside a portal.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
    }

    public boolean allowsScreenInPortal(boolean vanillaAllows) {
        return isEnabled() || vanillaAllows;
    }
}
