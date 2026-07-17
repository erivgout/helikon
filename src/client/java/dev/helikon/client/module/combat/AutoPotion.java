package dev.helikon.client.module.combat;

import dev.helikon.client.combat.PotionCandidate;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** Selects a player-owned restorative potion and invokes only the normal held-item use path. */
public final class AutoPotion extends Module {
    public enum Kind {
        SPLASH,
        DRINK,
        BOTH
    }

    public enum ActionType {
        NONE,
        SELECT_AND_USE,
        USE_SELECTED,
        RESTORE_SLOT
    }

    public record Context(int selectedSlot, double health, boolean screenOpen, boolean usingItem,
                          List<PotionCandidate> candidates) {
        public Context {
            if (selectedSlot < 0 || selectedSlot > 8 || !Double.isFinite(health) || health < 0.0D || candidates == null) {
                throw new IllegalArgumentException("auto-potion context is invalid");
            }
            candidates = List.copyOf(candidates);
        }
    }

    public record Action(ActionType type, int slot) {
        private static final Action NONE = new Action(ActionType.NONE, -1);

        public Action {
            if (type == null || (type != ActionType.NONE && (slot < 0 || slot > 8))) {
                throw new IllegalArgumentException("auto-potion action is invalid");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting healthThreshold;
    private final StringSetting whitelist;
    private final EnumSetting<Kind> kind;
    private final NumberSetting delayTicks;
    private int originalSlot = -1;
    private int selectedPotionSlot = -1;
    private long lastUseTick = -1L;

    public AutoPotion() {
        super("auto_potion", "AutoPotion", "Uses player-owned restorative potions through normal held-item interactions.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Use a configured restorative potion at or below this local health value.", 10.0D, 1.0D, 20.0D));
        whitelist = addSetting(new StringSetting("potion_whitelist", "Potion whitelist",
                "Comma-separated restorative potion IDs, such as healing,strong_healing.", "healing,strong_healing", 160, false));
        kind = addSetting(new EnumSetting<>("kind", "Potion kind", "Allow splash, drinkable, or both potion kinds.",
                Kind.class, Kind.SPLASH));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Delay", "Minimum ticks between normal potion uses.",
                20.0D, 5.0D, 200.0D));
    }

    public Action update(long tick, Context context) {
        if (tick < 0L || context == null) {
            throw new IllegalArgumentException("auto-potion inputs are invalid");
        }
        if (selectedPotionSlot >= 0) {
            if (context.usingItem()) {
                return Action.none();
            }
            return releaseOwnedSlot(context.selectedSlot());
        }
        if (!isEnabled() || context.screenOpen() || context.health() > healthThreshold.value()
                || (lastUseTick >= 0L && tick - lastUseTick < Math.round(delayTicks.value()))) {
            return Action.none();
        }
        PotionCandidate candidate = context.candidates().stream().filter(this::allowed).min(Comparator.comparingInt(PotionCandidate::slot))
                .orElse(null);
        if (candidate == null) {
            return Action.none();
        }
        originalSlot = context.selectedSlot();
        selectedPotionSlot = candidate.slot();
        lastUseTick = tick;
        return candidate.slot() == context.selectedSlot()
                ? new Action(ActionType.USE_SELECTED, candidate.slot())
                : new Action(ActionType.SELECT_AND_USE, candidate.slot());
    }

    public void onPlayerUnavailable() {
        originalSlot = -1;
        selectedPotionSlot = -1;
    }

    private Action releaseOwnedSlot(int currentSlot) {
        int restore = originalSlot;
        boolean ownsSelection = currentSlot == selectedPotionSlot && restore >= 0 && restore != currentSlot;
        originalSlot = -1;
        selectedPotionSlot = -1;
        return ownsSelection ? new Action(ActionType.RESTORE_SLOT, restore) : Action.none();
    }

    private boolean allowed(PotionCandidate candidate) {
        if (!candidate.restorative() || !allowedKind(candidate.kind())) {
            return false;
        }
        return whitelistTokens().contains(candidate.potionId().toLowerCase(Locale.ROOT));
    }

    private boolean allowedKind(PotionCandidate.Kind candidateKind) {
        return kind.value() == Kind.BOTH || (kind.value() == Kind.SPLASH && candidateKind == PotionCandidate.Kind.SPLASH)
                || (kind.value() == Kind.DRINK && candidateKind == PotionCandidate.Kind.DRINK);
    }

    private Set<String> whitelistTokens() {
        return Arrays.stream(whitelist.value().split(",")).map(String::trim).map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty()).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected void onDisable() {
        // The always-running local adapter restores a slot only while this module still owns it.
    }
}
