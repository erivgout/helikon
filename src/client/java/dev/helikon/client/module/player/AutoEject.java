package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Drops configured junk only from an open vanilla inventory screen. */
public final class AutoEject extends Module {
    private final StringSetting blacklist;
    private final BooleanSetting dropStack;
    private final NumberSetting delayTicks;
    private final StringSetting protectedSlots;
    private long nextActionTick;

    public AutoEject() {
        super("auto_eject", "AutoEject", "Drops configured junk through normal vanilla inventory clicks.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        blacklist = addSetting(new StringSetting("blacklist", "Item blacklist",
                "Comma-separated item IDs that may be dropped from an open inventory.",
                "minecraft:rotten_flesh,minecraft:poisonous_potato", 512, true));
        dropStack = addSetting(new BooleanSetting("drop_stack", "Drop stack",
                "Drop a whole matching stack instead of one item per action.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal drop clicks.",
                4.0D, 1.0D, 40.0D));
        protectedSlots = addSetting(new StringSetting("protected_slots", "Protected slots",
                "Comma-separated inventory slot numbers/ranges that AutoEject must never drop.", "0-8", 128, true));
    }

    public Optional<List<ContainerClick>> nextAction(long tick, List<InventoryItem> inventory) {
        if (!isEnabled() || tick < nextActionTick) {
            return Optional.empty();
        }
        Set<String> ids = InventorySlotRules.itemIds(blacklist.value());
        Set<Integer> protectedIndices = InventorySlotRules.slots(protectedSlots.value(), 35);
        Optional<InventoryItem> candidate = inventory.stream()
                .filter(item -> ids.contains(item.itemId()))
                .filter(item -> !protectedIndices.contains(item.inventorySlot()))
                .min(Comparator.comparingInt(InventoryItem::inventorySlot));
        if (candidate.isEmpty()) {
            return Optional.empty();
        }
        nextActionTick = tick + Math.round(delayTicks.value());
        return Optional.of(dropStack.value() ? ContainerClickSequence.throwStack(candidate.get().menuSlot())
                : ContainerClickSequence.throwOne(candidate.get().menuSlot()));
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }
}
