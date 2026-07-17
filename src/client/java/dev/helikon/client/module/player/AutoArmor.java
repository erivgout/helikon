package dev.helikon.client.module.player;

import dev.helikon.client.automation.ContainerClick;
import dev.helikon.client.automation.ContainerClickSequence;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Equips one strictly better armor item at a time through a conservative local UI policy. */
public final class AutoArmor extends Module {
    private final BooleanSetting preferDurability;
    private final BooleanSetting protectBindingCurse;
    private final NumberSetting delayTicks;
    private final NumberSetting minimumImprovement;
    private long nextActionTick;

    public AutoArmor() {
        super("auto_armor", "AutoArmor", "Equips a strictly better armor item through vanilla inventory clicks.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        preferDurability = addSetting(new BooleanSetting("prefer_durability", "Prefer durability",
                "Use remaining durability as a small tie-breaker between comparable armor.", true));
        protectBindingCurse = addSetting(new BooleanSetting("protect_binding_curse", "Protect Binding Curse",
                "Never replace equipped armor with the Binding Curse while this safety guard is enabled.", true));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal inventory swaps.",
                4.0D, 1.0D, 40.0D));
        minimumImprovement = addSetting(new NumberSetting("minimum_improvement", "Minimum improvement",
                "Only equip armor whose local score improves by more than this amount.", 0.01D, 0.0D, 5.0D));
    }

    public Optional<List<ContainerClick>> nextAction(long tick, List<ArmorCandidate> candidates,
                                                       Map<ArmorSlot, ArmorCandidate> equipped,
                                                       Map<ArmorSlot, Integer> destinationSlots) {
        if (!isEnabled() || tick < nextActionTick) {
            return Optional.empty();
        }
        Optional<ArmorSelection.Upgrade> upgrade = ArmorSelection.bestUpgrade(candidates, equipped, destinationSlots,
                preferDurability.value(), protectBindingCurse.value(), minimumImprovement.value());
        if (upgrade.isEmpty()) {
            return Optional.empty();
        }
        nextActionTick = tick + Math.round(delayTicks.value());
        ArmorSelection.Upgrade action = upgrade.get();
        return Optional.of(ContainerClickSequence.swap(action.sourceMenuSlot(), action.destinationMenuSlot()));
    }

    @Override
    protected void onDisable() {
        nextActionTick = 0L;
    }
}
