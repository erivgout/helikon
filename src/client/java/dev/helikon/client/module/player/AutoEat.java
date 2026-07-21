package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.Collectors;

/** Holds ordinary local Use with an existing safe hotbar food only when eating is warranted. */
public final class AutoEat extends Module {
    public enum FoodPriority {
        HOTBAR_ORDER,
        HIGHEST_NUTRITION,
        HIGHEST_SATURATION
    }

    public enum CombatRule {
        ALWAYS,
        PAUSE_WHILE_ATTACKING,
        PAUSE_WHILE_HURT
    }

    public record Context(int currentSlot, int hunger, float health, boolean attackHeld,
                          boolean recentlyHurt, boolean screenOpen, boolean useKeyDown, boolean usingItem,
                          List<FoodCandidate> candidates) {
        public Context {
            if (currentSlot < 0 || currentSlot >= 9 || hunger < 0 || hunger > 20
                    || !Float.isFinite(health) || health < 0.0F || candidates == null) {
                throw new IllegalArgumentException("Invalid local AutoEat context");
            }
            candidates = List.copyOf(candidates);
        }
    }

    public record Action(int selectSlot, boolean pressUse, boolean releaseUse, int restoreSlot) {
        private static final Action NONE = new Action(-1, false, false, -1);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting hungerThreshold;
    private final NumberSetting healthThreshold;
    private final EnumSetting<FoodPriority> foodPriority;
    private final StringSetting avoidedFoods;
    private final EnumSetting<CombatRule> combatRule;
    private final UseKeyAccess useKey;
    private int priorSlot = -1;
    private int selectedFoodSlot = -1;
    private boolean useHeldByModule;
    private boolean releaseRequested;

    public AutoEat(UseKeyAccess useKey) {
        super("auto_eat", "AutoEat", "Uses safe local hotbar food when hunger or health is low.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        this.useKey = java.util.Objects.requireNonNull(useKey, "useKey");
        hungerThreshold = addSetting(new NumberSetting("hunger_threshold", "Hunger threshold",
                "Begin eating at or below this hunger level, when food can be eaten.", 14.0, 0.0, 19.0));
        healthThreshold = addSetting(new NumberSetting("health_threshold", "Health threshold",
                "Also permit eating at or below this health level when hunger is not full.", 8.0, 0.0, 20.0));
        foodPriority = addSetting(new EnumSetting<>("food_priority", "Food priority",
                "Choose the safe hotbar-food ordering policy.", FoodPriority.class, FoodPriority.HIGHEST_NUTRITION));
        avoidedFoods = addSetting(new StringSetting("avoided_foods", "Avoided foods",
                "Comma-separated item identifiers that AutoEat must never select.", "", 255, true));
        combatRule = addSetting(new EnumSetting<>("combat_rule", "Combat interruption",
                "Choose when recent combat locally pauses AutoEat.", CombatRule.class, CombatRule.PAUSE_WHILE_ATTACKING));
    }

    /** Computes selection, normal Use-key ownership, and slot restoration from local facts. */
    public Action update(Context context) {
        if (releaseRequested) {
            releaseRequested = false;
            return release(context.currentSlot());
        }
        if (!isEnabled() || !shouldEat(context) || isCombatPaused(context)) {
            return release(context.currentSlot());
        }
        if (selectedFoodSlot >= 0 && context.currentSlot() != selectedFoodSlot) {
            return releaseWithoutRestore();
        }
        if (!useHeldByModule && (context.useKeyDown() || context.usingItem())) {
            return Action.none();
        }

        List<FoodCandidate> eligibleCandidates = context.hunger() == 20
                ? context.candidates().stream().filter(FoodCandidate::alwaysEdible).toList()
                : context.candidates();
        OptionalInt selected = FoodSelection.bestSlot(eligibleCandidates, avoidedFoodIds(), foodPriority.value());
        if (selected.isEmpty()) {
            return release(context.currentSlot());
        }
        int foodSlot = selected.getAsInt();
        if (priorSlot < 0) {
            priorSlot = context.currentSlot();
        }
        selectedFoodSlot = foodSlot;
        if (useHeldByModule) {
            return Action.none();
        }
        useHeldByModule = true;
        return new Action(foodSlot == context.currentSlot() ? -1 : foodSlot, true, false, -1);
    }

    /** Runs the validated policy and applies only its normal local Use-key action. */
    public Action tick(Context context) {
        Action action = update(context);
        if (action.releaseUse() && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        if (action.pressUse()) {
            useKey.setDown(true);
        }
        return action;
    }

    /** Releases only module-owned Use input if the local player disappears. */
    public Action onPlayerUnavailable() {
        boolean releaseUse = useHeldByModule;
        if (releaseUse && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        clearOwnership();
        releaseRequested = false;
        return releaseUse ? new Action(-1, false, true, -1) : Action.none();
    }

    /** Whether AutoEat currently owns Use and is waiting for normal item consumption to finish. */
    public boolean isEating() {
        return isEnabled() && useHeldByModule;
    }

    @Override
    protected void onDisable() {
        if (useHeldByModule && !useKey.isPhysicallyDown()) {
            useKey.setDown(false);
        }
        useHeldByModule = false;
        if (priorSlot >= 0) {
            releaseRequested = true;
        }
    }

    private boolean shouldEat(Context context) {
        boolean hungryEnough = context.hunger() <= (int) Math.round(hungerThreshold.value());
        boolean healthLow = context.health() <= healthThreshold.value();
        if (context.screenOpen()) {
            return false;
        }
        if (context.hunger() < 20) {
            return hungryEnough || healthLow;
        }
        return healthLow && context.candidates().stream().anyMatch(FoodCandidate::alwaysEdible);
    }

    private boolean isCombatPaused(Context context) {
        return switch (combatRule.value()) {
            case ALWAYS -> false;
            case PAUSE_WHILE_ATTACKING -> context.attackHeld();
            case PAUSE_WHILE_HURT -> context.recentlyHurt();
        };
    }

    private Set<String> avoidedFoodIds() {
        return Arrays.stream(avoidedFoods.value().split(","))
                .map(itemId -> itemId.trim().toLowerCase(Locale.ROOT))
                .filter(itemId -> !itemId.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private Action release(int currentSlot) {
        boolean releaseUse = useHeldByModule;
        int restoreSlot = priorSlot >= 0 && currentSlot == selectedFoodSlot ? priorSlot : -1;
        clearOwnership();
        return releaseUse || restoreSlot >= 0 ? new Action(-1, false, releaseUse, restoreSlot) : Action.none();
    }

    private Action releaseWithoutRestore() {
        boolean releaseUse = useHeldByModule;
        clearOwnership();
        return releaseUse ? new Action(-1, false, true, -1) : Action.none();
    }

    private void clearOwnership() {
        priorSlot = -1;
        selectedFoodSlot = -1;
        useHeldByModule = false;
    }

    /** Narrow Minecraft-free port for normal local Use-key ownership. */
    public interface UseKeyAccess {
        boolean isPhysicallyDown();

        void setDown(boolean value);
    }
}
