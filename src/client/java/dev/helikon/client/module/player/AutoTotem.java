package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Moves an existing totem to the offhand only when local safety thresholds require it. */
public final class AutoTotem extends Module {
    public record Context(float health, float fallDistance, boolean offhandIsTotem, String offhandItemId,
                          int offhandMenuSlot, List<InventoryItem> inventory) {
        public Context {
            if (!Float.isFinite(health) || health < 0.0F || !Float.isFinite(fallDistance) || fallDistance < 0.0F
                    || offhandMenuSlot < 0) {
                throw new IllegalArgumentException("totem context is invalid");
            }
            offhandItemId = Objects.requireNonNullElse(offhandItemId, "");
            inventory = List.copyOf(Objects.requireNonNull(inventory, "inventory"));
        }
    }

    private static final String TOTEM_ID = "minecraft:totem_of_undying";

    private final NumberSetting healthThreshold;
    private final NumberSetting fallThreshold;
    private final BooleanSetting restorePreviousOffhand;
    private final NumberSetting delayTicks;
    private long nextActionTick;
    private int restoreMenuSlot = -1;
    private String expectedPriorItemId = "";
    private boolean expectedPriorOffhandEmpty;
    private boolean restoreRequested;

    public AutoTotem() {
        super("auto_totem", "AutoTotem", "Moves an existing totem to the offhand at configured local thresholds.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Use an offhand totem at or below this local health value.", 8.0D, 1.0D, 20.0D));
        fallThreshold = addSetting(new NumberSetting("fall_threshold", "Fall threshold",
                "Use an offhand totem at or above this local fall distance.", 10.0D, 1.0D, 80.0D));
        restorePreviousOffhand = addSetting(new BooleanSetting("restore_previous_offhand", "Restore previous offhand",
                "Restore the preexisting offhand item if it remains in the source slot after safety clears.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal inventory swaps.",
                4.0D, 1.0D, 40.0D));
    }

    public boolean needsTotem(float health, float fallDistance) {
        if (!Float.isFinite(health) || !Float.isFinite(fallDistance)) {
            throw new IllegalArgumentException("health and fallDistance must be finite");
        }
        return health <= healthThreshold.value() || fallDistance >= fallThreshold.value();
    }

    public Optional<List<ContainerClick>> nextAction(long tick, Context context) {
        Objects.requireNonNull(context, "context");
        if (tick < nextActionTick) {
            return Optional.empty();
        }
        if (isEnabled() && needsTotem(context.health(), context.fallDistance())) {
            if (context.offhandIsTotem()) {
                return Optional.empty();
            }
            Optional<InventoryItem> totem = context.inventory().stream()
                    .filter(item -> TOTEM_ID.equals(item.itemId()))
                    .min(Comparator.comparingInt(InventoryItem::inventorySlot));
            if (totem.isEmpty()) {
                return Optional.empty();
            }
            if (restorePreviousOffhand.value()) {
                restoreMenuSlot = totem.get().menuSlot();
                expectedPriorItemId = context.offhandItemId();
                expectedPriorOffhandEmpty = context.offhandItemId().isBlank();
                restoreRequested = false;
            }
            nextActionTick = tick + Math.round(delayTicks.value());
            return Optional.of(ContainerClickSequence.swap(totem.get().menuSlot(), context.offhandMenuSlot()));
        }
        if ((isEnabled() || restoreRequested) && restoreMenuSlot >= 0 && context.offhandIsTotem()) {
            if (expectedPriorOffhandEmpty) {
                boolean sourceIsStillEmpty = context.inventory().stream()
                        .noneMatch(item -> item.menuSlot() == restoreMenuSlot);
                if (sourceIsStillEmpty) {
                    int sourceMenuSlot = restoreMenuSlot;
                    clearRestore();
                    nextActionTick = tick + Math.round(delayTicks.value());
                    return Optional.of(ContainerClickSequence.swap(sourceMenuSlot, context.offhandMenuSlot()));
                }
                clearRestore();
                return Optional.empty();
            }
            Optional<InventoryItem> prior = context.inventory().stream()
                    .filter(item -> item.menuSlot() == restoreMenuSlot)
                    .filter(item -> item.itemId().equals(expectedPriorItemId))
                    .findFirst();
            if (prior.isPresent()) {
                clearRestore();
                nextActionTick = tick + Math.round(delayTicks.value());
                return Optional.of(ContainerClickSequence.swap(prior.get().menuSlot(), context.offhandMenuSlot()));
            }
            clearRestore();
        }
        return Optional.empty();
    }

    @Override
    protected void onDisable() {
        restoreRequested = restoreMenuSlot >= 0;
        nextActionTick = 0L;
    }

    private void clearRestore() {
        restoreMenuSlot = -1;
        expectedPriorItemId = "";
        expectedPriorOffhandEmpty = false;
        restoreRequested = false;
    }
}
