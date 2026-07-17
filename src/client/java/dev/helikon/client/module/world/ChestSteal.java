package dev.helikon.client.module.world;

import dev.helikon.client.automation.ContainerClick;
import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.module.player.InventorySlotRules;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Transfers one eligible item from an open vanilla chest menu through QUICK_MOVE. */
public final class ChestSteal extends Module {
    public enum Priority {
        HIGHEST_VALUE,
        LOWEST_SLOT
    }

    public enum ActionType {
        QUICK_MOVE,
        CLOSE
    }

    public record Action(ActionType type, List<ContainerClick> clicks) {
        public Action {
            if (type == null || clicks == null) {
                throw new IllegalArgumentException("action fields must not be null");
            }
            clicks = List.copyOf(clicks);
        }
    }

    private final NumberSetting delayTicks;
    private final StringSetting whitelist;
    private final StringSetting blacklist;
    private final EnumSetting<Priority> priority;
    private final BooleanSetting closeAfterCompletion;
    private long nextActionTick;
    private int activeMenuId = Integer.MIN_VALUE;
    private boolean closeRequested;

    public ChestSteal() {
        super("chest_steal", "ChestSteal", "Transfers eligible chest items through normal vanilla quick-move clicks.",
                ModuleCategory.WORLD, false, Keybind.unbound());
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal chest clicks.",
                4.0D, 1.0D, 40.0D));
        whitelist = addSetting(new StringSetting("whitelist", "Whitelist",
                "Optional comma-separated item IDs to take; blank allows all non-blacklisted items.", "", 512, true));
        blacklist = addSetting(new StringSetting("blacklist", "Blacklist",
                "Comma-separated item IDs to leave in the chest.", "", 512, true));
        priority = addSetting(new EnumSetting<>("priority", "Priority", "Choose the order of eligible chest items.",
                Priority.class, Priority.HIGHEST_VALUE));
        closeAfterCompletion = addSetting(new BooleanSetting("close_after_completion", "Close after completion",
                "Close the normal chest menu after all eligible items were attempted.", false));
    }

    public Optional<Action> nextAction(long tick, int menuId, List<ChestItem> items) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        if (menuId != activeMenuId) {
            activeMenuId = menuId;
            closeRequested = false;
            nextActionTick = 0L;
        }
        if (tick < nextActionTick) {
            return Optional.empty();
        }
        Set<String> allowed = InventorySlotRules.itemIds(whitelist.value());
        Set<String> blocked = InventorySlotRules.itemIds(blacklist.value());
        Optional<ChestItem> candidate = items.stream()
                .filter(item -> allowed.isEmpty() || allowed.contains(item.itemId()))
                .filter(item -> !blocked.contains(item.itemId()))
                .min(order());
        if (candidate.isPresent()) {
            nextActionTick = tick + Math.round(delayTicks.value());
            return Optional.of(new Action(ActionType.QUICK_MOVE, ContainerClickSequence.quickMove(candidate.get().menuSlot())));
        }
        if (closeAfterCompletion.value() && !closeRequested) {
            closeRequested = true;
            return Optional.of(new Action(ActionType.CLOSE, List.of()));
        }
        return Optional.empty();
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
        activeMenuId = Integer.MIN_VALUE;
        closeRequested = false;
    }

    private Comparator<ChestItem> order() {
        return switch (priority.value()) {
            case HIGHEST_VALUE -> Comparator.comparingInt(ChestItem::priority).reversed()
                    .thenComparing(ChestItem::itemId).thenComparingInt(ChestItem::menuSlot);
            case LOWEST_SLOT -> Comparator.comparingInt(ChestItem::menuSlot);
        };
    }
}
