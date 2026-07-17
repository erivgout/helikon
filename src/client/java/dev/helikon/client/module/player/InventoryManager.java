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

/** Makes one reversible normal-inventory arrangement or junk-drop request at a time. */
public final class InventoryManager extends Module {
    private final BooleanSetting sortInventory;
    private final BooleanSetting dropJunk;
    private final StringSetting junkItems;
    private final StringSetting preferredHotbar;
    private final BooleanSetting preserveNamed;
    private final BooleanSetting preserveEnchanted;
    private final NumberSetting minimumDurability;
    private final NumberSetting delayTicks;
    private long nextActionTick;

    public InventoryManager() {
        super("inventory_manager", "InventoryManager", "Sorts and tidies an open vanilla inventory one action at a time.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        sortInventory = addSetting(new BooleanSetting("sort_inventory", "Sort inventory",
                "Sort movable ordinary items by item ID while the vanilla inventory is open.", false));
        dropJunk = addSetting(new BooleanSetting("drop_junk", "Drop junk", "Drop configured junk through a normal click.", false));
        junkItems = addSetting(new StringSetting("junk_items", "Junk items", "Comma-separated junk item IDs.",
                "minecraft:rotten_flesh,minecraft:poisonous_potato", 512, true));
        preferredHotbar = addSetting(new StringSetting("preferred_hotbar", "Preferred hotbar",
                "Comma-separated exact item_id=hotbar_slot preferences, used only for empty target slots.",
                "", 512, true));
        preserveNamed = addSetting(new BooleanSetting("preserve_named", "Preserve named items",
                "Never move or drop items with custom names.", true));
        preserveEnchanted = addSetting(new BooleanSetting("preserve_enchanted", "Preserve enchanted items",
                "Never move or drop enchanted items.", true));
        minimumDurability = addSetting(new NumberSetting("minimum_durability", "Minimum durability",
                "Protect damageable items below this remaining-durability guard.", 8.0D, 0.0D, 2_032.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal inventory clicks.",
                4.0D, 1.0D, 40.0D));
    }

    public Optional<List<ContainerClick>> nextAction(long tick, List<InventoryItem> items,
                                                       Map<Integer, Integer> menuSlotsByInventorySlot) {
        if (!isEnabled() || tick < nextActionTick) {
            return Optional.empty();
        }
        InventoryManagerPolicy.Options options = new InventoryManagerPolicy.Options(sortInventory.value(), dropJunk.value(),
                InventorySlotRules.itemIds(junkItems.value()), PreferredHotbarSlots.parse(preferredHotbar.value()),
                preserveNamed.value(), preserveEnchanted.value(), (int) Math.round(minimumDurability.value()));
        Optional<InventoryManagerPolicy.Action> action = InventoryManagerPolicy.nextAction(items,
                menuSlotsByInventorySlot, options);
        if (action.isEmpty()) {
            return Optional.empty();
        }
        nextActionTick = tick + Math.round(delayTicks.value());
        return Optional.of(switch (action.get().type()) {
            case SWAP -> ContainerClickSequence.swap(action.get().sourceMenuSlot(), action.get().destinationMenuSlot());
            case DROP_STACK -> ContainerClickSequence.throwStack(action.get().sourceMenuSlot());
        });
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }
}
