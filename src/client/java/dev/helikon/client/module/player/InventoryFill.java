package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Keeps configured hotbar slots stocked by moving matching reserves into them through
 * ordinary vanilla inventory clicks. Each action is a single normal swap that fills an
 * empty slot or merges onto a partial one; the server stays authoritative and may reject
 * or correct any move.
 */
public final class InventoryFill extends Module {
    private final StringSetting fillTargets;
    private final NumberSetting minimumCount;
    private final BooleanSetting preserveNamed;
    private final BooleanSetting preserveEnchanted;
    private final NumberSetting delayTicks;
    private long nextActionTick;

    public InventoryFill() {
        super("inventory_fill", "InventoryFill",
                "Refills configured hotbar slots from matching inventory reserves through normal vanilla clicks.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        fillTargets = addSetting(new StringSetting("fill_targets", "Fill targets",
                "Comma-separated item_id=hotbar_slot pairs to keep stocked (e.g. minecraft:cobblestone=0).",
                "", 512, true));
        minimumCount = addSetting(new NumberSetting("minimum_count", "Minimum count",
                "Refill a target slot whenever it holds fewer than this many of its configured item.",
                1.0D, 1.0D, 64.0D));
        preserveNamed = addSetting(new BooleanSetting("preserve_named", "Preserve named items",
                "Never draw custom-named items from reserves as a fill source.", true));
        preserveEnchanted = addSetting(new BooleanSetting("preserve_enchanted", "Preserve enchanted items",
                "Never draw enchanted items from reserves as a fill source.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay",
                "Minimum ticks between normal refill clicks. The server may still reject, correct, or rubber-band a move.",
                2.0D, 1.0D, 40.0D));
    }

    public Optional<List<ContainerClick>> nextAction(long tick, List<InventoryItem> items,
                                                     Map<Integer, Integer> menuSlotsByInventorySlot) {
        if (!isEnabled() || tick < nextActionTick) {
            return Optional.empty();
        }
        Map<String, Integer> targets = PreferredHotbarSlots.parse(fillTargets.value());
        Optional<InventoryFillPolicy.Fill> fill = InventoryFillPolicy.nextFill(items, menuSlotsByInventorySlot, targets,
                preserveNamed.value(), preserveEnchanted.value(), (int) Math.round(minimumCount.value()));
        if (fill.isEmpty()) {
            return Optional.empty();
        }
        nextActionTick = tick + Math.round(delayTicks.value());
        return Optional.of(ContainerClickSequence.swap(fill.get().sourceMenuSlot(), fill.get().targetMenuSlot()));
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }
}
