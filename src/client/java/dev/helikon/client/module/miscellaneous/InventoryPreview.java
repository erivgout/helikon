package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Configures a local read-only grid preview of the player's non-equipment inventory. */
public final class InventoryPreview extends Module {
    private final NumberSetting rows;
    private final BooleanSetting includeHotbar;

    public InventoryPreview() {
        super("inventory_preview", "Inventory Preview", "Shows a local read-only player inventory grid in the HUD.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        rows = addSetting(new NumberSetting("rows", "Storage rows",
                "Number of main-inventory rows to show locally.", 3.0D, 1.0D, 3.0D));
        includeHotbar = addSetting(new BooleanSetting("include_hotbar", "Include hotbar",
                "Append the local hotbar as the final preview row.", false));
    }

    public int rows() {
        return (int) Math.round(rows.value());
    }

    public boolean includeHotbar() {
        return includeHotbar.value();
    }
}
