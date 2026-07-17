package dev.helikon.client.module.miscellaneous;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Objects;

/** Selects low-durability local equipment facts for a compact warning HUD. */
public final class DurabilityWarnings extends Module {
    /** One already-observed damageable item. */
    public record Item(String label, int remaining, int maximum) {
        public Item {
            label = Objects.requireNonNullElse(label, "").trim();
            if (label.isEmpty() || label.length() > 32 || remaining < 0 || maximum <= 0 || remaining > maximum) {
                throw new IllegalArgumentException("Invalid durability item fact");
            }
        }
    }

    private final NumberSetting thresholdPercent;
    private final BooleanSetting heldItem;
    private final BooleanSetting armor;

    public DurabilityWarnings() {
        super("durability_warnings", "Durability Warnings", "Shows local low-durability held-item and armor warnings.",
                ModuleCategory.MISCELLANEOUS, false, Keybind.unbound());
        thresholdPercent = addSetting(new NumberSetting("threshold_percent", "Threshold percent",
                "Warn when remaining durability is at or below this percentage.", 10.0D, 1.0D, 50.0D));
        heldItem = addSetting(new BooleanSetting("held_item", "Held item", "Include the current main-hand item.", true));
        armor = addSetting(new BooleanSetting("armor", "Armor", "Include local worn armor pieces.", true));
    }

    /** Returns at most five local warnings in caller-provided display order. */
    public List<Item> warnings(List<Item> items) {
        Objects.requireNonNull(items, "items");
        if (!isEnabled()) {
            return List.of();
        }
        long threshold = Math.round(thresholdPercent.value());
        return items.stream().filter(item -> isLow(item, threshold)).limit(5).toList();
    }

    public boolean includeHeldItem() {
        return heldItem.value();
    }

    public boolean includeArmor() {
        return armor.value();
    }

    public int thresholdPercent() {
        return (int) Math.round(thresholdPercent.value());
    }

    private static boolean isLow(Item item, long thresholdPercent) {
        return (long) item.remaining() * 100L <= (long) item.maximum() * thresholdPercent;
    }
}
