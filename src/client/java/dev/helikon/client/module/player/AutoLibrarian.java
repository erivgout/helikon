package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.List;
import java.util.Locale;

/** Minecraft-free desired-trade and bounded reroll state machine. */
public final class AutoLibrarian extends Module {
    public enum Phase { WAITING_FOR_TRADE, BREAK_LECTERN, PLACE_LECTERN, MATCH_FOUND }

    private final StringSetting desiredEnchantment;
    private final IntegerSetting maximumEmeralds;
    private final IntegerSetting radius;
    private Phase phase = Phase.WAITING_FOR_TRADE;

    public AutoLibrarian() {
        super("auto_librarian", "AutoLibrarian",
                "Rerolls a nearby librarian lectern until a configured enchanted-book trade appears.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        desiredEnchantment = addSetting(new StringSetting("desired_enchantment", "Desired enchantment",
                "Registry ID such as minecraft:mending.", "minecraft:mending", 80, false));
        maximumEmeralds = addSetting(new IntegerSetting("maximum_emeralds", "Maximum emeralds",
                "Reject matching offers above this base emerald cost.", 64, 1, 64));
        radius = addSetting(new IntegerSetting("radius", "Lectern radius",
                "Maximum loaded distance used to find the nearby lectern.", 5, 2, 8));
    }

    public Decision inspect(List<Offer> offers) {
        if (!isEnabled() || offers == null) {
            return Decision.WAIT;
        }
        String desired = desiredEnchantment.value().toLowerCase(Locale.ROOT);
        boolean found = offers.stream().anyMatch(offer -> offer.emeraldCost() <= maximumEmeralds.value()
                && offer.enchantments().stream().map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.equals(desired)));
        phase = found ? Phase.MATCH_FOUND : Phase.BREAK_LECTERN;
        return found ? Decision.FOUND : Decision.REROLL;
    }

    public void markBroken() {
        if (phase == Phase.BREAK_LECTERN) {
            phase = Phase.PLACE_LECTERN;
        }
    }

    public void markPlaced() {
        phase = Phase.WAITING_FOR_TRADE;
    }

    public Phase phase() {
        return phase;
    }

    public int radius() {
        return radius.value();
    }

    public void reset() {
        phase = Phase.WAITING_FOR_TRADE;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    public enum Decision { WAIT, FOUND, REROLL }

    public record Offer(List<String> enchantments, int emeraldCost) {
        public Offer {
            enchantments = List.copyOf(enchantments);
            if (emeraldCost < 0) {
                throw new IllegalArgumentException("emeraldCost must be non-negative");
            }
        }
    }
}
