package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

/** Explicit opt-in gate for the local middle-click friend gesture. */
public final class OneClickFriends extends Module {
    public OneClickFriends() {
        super("one_click_friends", "OneClickFriends", "Lets middle-click toggle a targeted local friend entry.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
    }

    /** The gesture adapter must call this after recording every physical press edge. */
    public boolean allowsToggle() {
        return isEnabled();
    }
}
