package dev.helikon.client.module.player;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Selects one harmful-only splash potion and one eligible local target, then requests a normal
 * held-item throw. Minecraft-facing potion inspection and use remain in the narrow adapter.
 */
public final class ThrowDebuff extends Module {
    private static final double POTION_SPEED = 0.5D;
    private static final double POTION_GRAVITY = 0.05D;
    private static final float VANILLA_PITCH_OFFSET = -20.0F;

    public record PotionCandidate(int slot, String potionId, boolean harmfulOnly) {
        public PotionCandidate {
            if (slot < 0 || slot > 8 || potionId == null || potionId.isBlank()) {
                throw new IllegalArgumentException("Invalid debuff-potion candidate");
            }
        }
    }

    public record Context(int selectedSlot, boolean screenOpen, boolean usingItem,
                          List<PotionCandidate> potions, List<CombatTarget> targets) {
        public Context {
            if (selectedSlot < 0 || selectedSlot > 8) {
                throw new IllegalArgumentException("Selected slot must be a hotbar index");
            }
            potions = List.copyOf(Objects.requireNonNull(potions, "potions"));
            targets = List.copyOf(Objects.requireNonNull(targets, "targets"));
        }
    }

    public enum ActionType {
        NONE,
        THROW_SELECTED,
        SELECT_AND_THROW,
        RESTORE_SLOT
    }

    public record Action(ActionType type, int slot, String targetId, float yaw, float pitch, boolean rotate) {
        private static final Action NONE = new Action(ActionType.NONE, -1, "", 0.0F, 0.0F, false);

        public Action {
            Objects.requireNonNull(type, "type");
            Objects.requireNonNull(targetId, "targetId");
            if ((type == ActionType.NONE && slot != -1)
                    || (type != ActionType.NONE && (slot < 0 || slot > 8))
                    || !Float.isFinite(yaw) || !Float.isFinite(pitch) || pitch < -90.0F || pitch > 90.0F) {
                throw new IllegalArgumentException("Invalid ThrowDebuff action");
            }
        }

        public static Action none() {
            return NONE;
        }
    }

    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting delayTicks;
    private final BooleanSetting rotate;
    private final BooleanSetting restoreSlot;
    private final StringSetting potionWhitelist;
    private int originalSlot = -1;
    private int selectedPotionSlot = -1;
    private long lastThrowTick = -1L;

    public ThrowDebuff() {
        super("throw_debuff", "ThrowDebuff",
                "Throws player-provided harmful splash potions at eligible nearby opponents.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Target non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Target hostile mobs.", false));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Target passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never throw at locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range",
                "Maximum locally observed target distance.", 6.0D, 2.0D, 12.0D));
        delayTicks = addSetting(new NumberSetting("delay_ticks", "Throw delay",
                "Minimum client ticks between ordinary potion throws.", 20.0D, 5.0D, 200.0D));
        rotate = addSetting(new BooleanSetting("rotate", "Rotate",
                "Aim the local view using the verified splash-potion trajectory before throwing.", true));
        restoreSlot = addSetting(new BooleanSetting("restore_slot", "Restore slot",
                "Restore the previously selected hotbar slot after a throw.", true));
        potionWhitelist = addSetting(new StringSetting("potion_whitelist", "Potion whitelist",
                "Optional comma-separated base potion IDs; blank allows every harmful-only splash potion.",
                "", 256, true));
    }

    /** Returns at most one selection/use or owned-slot restoration request for a client tick. */
    public Action update(long clientTick, Context context) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        Context current = Objects.requireNonNull(context, "context");
        if (selectedPotionSlot >= 0) {
            return releaseOwnedSlot(current.selectedSlot());
        }
        if (!isEnabled() || current.screenOpen() || current.usingItem()
                || (lastThrowTick >= 0L && clientTick - lastThrowTick < Math.round(delayTicks.value()))) {
            return Action.none();
        }

        CombatTarget target = selectTarget(current.targets());
        PotionCandidate potion = selectPotion(current.potions());
        if (target == null || potion == null) {
            return Action.none();
        }

        CombatAim.Rotation trajectory = CombatAim.predictedRotation(target, POTION_SPEED, POTION_GRAVITY, true);
        float playerPitch = clampPitch(trajectory.pitch() - VANILLA_PITCH_OFFSET);
        originalSlot = current.selectedSlot();
        selectedPotionSlot = potion.slot();
        lastThrowTick = clientTick;
        ActionType action = potion.slot() == current.selectedSlot()
                ? ActionType.THROW_SELECTED
                : ActionType.SELECT_AND_THROW;
        return new Action(action, potion.slot(), target.id(), trajectory.yaw(), playerPitch, rotate.value());
    }

    public void onPlayerUnavailable() {
        originalSlot = -1;
        selectedPotionSlot = -1;
        lastThrowTick = -1L;
    }

    private CombatTarget selectTarget(List<CombatTarget> targets) {
        CombatTargetFilter.Options options = new CombatTargetFilter.Options(
                players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, range.value(), 180.0D, true
        );
        return CombatTargetFilter.ordered(targets, options, CombatTargetFilter.Priority.DISTANCE)
                .stream().findFirst().orElse(null);
    }

    private PotionCandidate selectPotion(List<PotionCandidate> potions) {
        Set<String> whitelist = whitelistTokens();
        return potions.stream()
                .filter(PotionCandidate::harmfulOnly)
                .filter(potion -> whitelist.isEmpty()
                        || whitelist.contains(potion.potionId().toLowerCase(Locale.ROOT)))
                .min(Comparator.comparingInt(PotionCandidate::slot))
                .orElse(null);
    }

    private Action releaseOwnedSlot(int currentSlot) {
        int restore = originalSlot;
        int potionSlot = selectedPotionSlot;
        originalSlot = -1;
        selectedPotionSlot = -1;
        boolean owns = restoreSlot.value() && restore >= 0 && restore != potionSlot && currentSlot == potionSlot;
        return owns ? new Action(ActionType.RESTORE_SLOT, restore, "", 0.0F, 0.0F, false) : Action.none();
    }

    private Set<String> whitelistTokens() {
        return Arrays.stream(potionWhitelist.value().split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static float clampPitch(float pitch) {
        return Math.max(-90.0F, Math.min(90.0F, pitch));
    }

    @Override
    protected void onEnable() {
        if (selectedPotionSlot < 0) {
            lastThrowTick = -1L;
        }
    }

    @Override
    protected void onDisable() {
        if (selectedPotionSlot < 0) {
            lastThrowTick = -1L;
        }
    }
}
