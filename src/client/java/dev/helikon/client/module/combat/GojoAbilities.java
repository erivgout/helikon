package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Selects a target and requests one player-provided vanilla item ability. */
public final class GojoAbilities extends Module {
    public enum Ability { RED, BLUE, PURPLE }

    private final EnumSetting<Ability> ability;
    private final NumberSetting range;
    private final IntegerSetting cooldownTicks;
    private final BooleanSetting sendIncantation;
    private long lastUseTick = -1;

    public GojoAbilities() {
        super("gojo_abilities", "Gojo Abilities",
                "Uses player-provided vanilla items as bounded Red, Blue, or Purple combat actions.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        ability = addSetting(new EnumSetting<>("ability", "Ability", "Vanilla-item ability to attempt.",
                Ability.class, Ability.RED));
        range = addSetting(new NumberSetting("range", "Range", "Maximum loaded non-friend player distance.",
                12.0, 3.0, 24.0));
        cooldownTicks = addSetting(new IntegerSetting("cooldown_ticks", "Cooldown",
                "Minimum ticks between activation attempts.", 40, 10, 200));
        sendIncantation = addSetting(new BooleanSetting("send_incantation", "Send incantation",
                "Send a short original ability line through ordinary chat.", false));
    }

    public Optional<Action> next(long tick, boolean screenOpen, List<CombatTarget> targets) {
        if (!isEnabled() || screenOpen || lastUseTick >= 0 && tick - lastUseTick < cooldownTicks.value()) {
            return Optional.empty();
        }
        return targets.stream()
                .filter(target -> target.type() == CombatEntityType.PLAYER && target.alive()
                        && !target.friend() && !target.suspectedBot() && target.lineOfSight()
                        && target.distance() <= range.value())
                .min(Comparator.comparingDouble(CombatTarget::distance))
                .map(target -> new Action(ability.value(), target, sendIncantation.value(),
                        incantation(ability.value())));
    }

    public void markUsed(long tick) {
        lastUseTick = tick;
    }

    public void reset() {
        lastUseTick = -1;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private static String incantation(Ability ability) {
        return switch (ability) {
            case RED -> "Helikon technique: reversal red.";
            case BLUE -> "Helikon technique: convergence blue.";
            case PURPLE -> "Helikon technique: hollow purple.";
        };
    }

    public record Action(Ability ability, CombatTarget target, boolean sendIncantation, String incantation) {
    }
}
