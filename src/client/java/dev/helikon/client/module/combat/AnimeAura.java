package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatEntityType;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Minecraft-free state machine for a bounded aerial teleport/attack combo. */
public final class AnimeAura extends Module {
    public enum Stage { IDLE, APPROACH, LAUNCHER, COMBO, FINISHER, RECOVERY }

    private final NumberSetting targetRange;
    private final NumberSetting orbitRadius;
    private final NumberSetting safetyHeight;
    private final IntegerSetting attackDelay;
    private final IntegerSetting comboLength;
    private Stage stage = Stage.IDLE;
    private String targetId;
    private int comboHits;
    private long lastActionTick = -1L;

    public AnimeAura() {
        super("anime_aura", "Anime Aura", "Runs a bounded TP-Aura-style aerial combo state machine.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        targetRange = addSetting(new NumberSetting("target_range", "Target range",
                "Maximum loaded target distance.", 12.0, 4.0, 24.0));
        orbitRadius = addSetting(new NumberSetting("orbit_radius", "Orbit radius",
                "Horizontal distance maintained around the target.", 2.2, 1.5, 3.0));
        safetyHeight = addSetting(new NumberSetting("safety_height", "Safety height",
                "Maximum height above the combo origin.", 12.0, 4.0, 32.0));
        attackDelay = addSetting(new IntegerSetting("attack_delay", "Attack delay",
                "Ticks between ordinary attack attempts.", 5, 2, 20));
        comboLength = addSetting(new IntegerSetting("combo_length", "Combo length",
                "Ordinary combo hits before the finisher.", 4, 1, 12));
    }

    public Optional<Action> next(long tick, boolean attackReady, List<CombatTarget> candidates) {
        if (!isEnabled()) {
            reset();
            return Optional.empty();
        }
        CombatTarget target = lockedTarget(candidates);
        if (target == null) {
            reset();
            return Optional.empty();
        }
        if (stage == Stage.IDLE) {
            targetId = target.id();
            stage = Stage.APPROACH;
            return Optional.of(new Action(Stage.APPROACH, target, comboHits, false,
                    orbitRadius.value(), safetyHeight.value()));
        }
        if (lastActionTick >= 0 && tick - lastActionTick < attackDelay.value()) {
            return Optional.empty();
        }
        if (!attackReady && stage != Stage.APPROACH && stage != Stage.RECOVERY) {
            return Optional.empty();
        }
        return Optional.of(new Action(stage, target, comboHits,
                stage == Stage.LAUNCHER || stage == Stage.COMBO || stage == Stage.FINISHER,
                orbitRadius.value(), safetyHeight.value()));
    }

    public void markSuccessful(long tick, Stage completed) {
        lastActionTick = tick;
        stage = switch (completed) {
            case APPROACH -> Stage.LAUNCHER;
            case LAUNCHER -> Stage.COMBO;
            case COMBO -> {
                comboHits++;
                yield comboHits >= comboLength.value() ? Stage.FINISHER : Stage.COMBO;
            }
            case FINISHER -> Stage.RECOVERY;
            case RECOVERY -> Stage.IDLE;
            case IDLE -> Stage.APPROACH;
        };
        if (stage == Stage.IDLE) {
            targetId = null;
            comboHits = 0;
        }
    }

    public Stage stage() {
        return stage;
    }

    public void reset() {
        stage = Stage.IDLE;
        targetId = null;
        comboHits = 0;
        lastActionTick = -1L;
    }

    @Override
    protected void onDisable() {
        reset();
    }

    private CombatTarget lockedTarget(List<CombatTarget> candidates) {
        if (candidates == null) {
            return null;
        }
        if (targetId != null) {
            CombatTarget locked = candidates.stream().filter(target -> target.id().equals(targetId))
                    .filter(this::eligible).findFirst().orElse(null);
            if (locked != null) {
                return locked;
            }
        }
        return candidates.stream().filter(this::eligible)
                .min(Comparator.comparingDouble(CombatTarget::distance)).orElse(null);
    }

    private boolean eligible(CombatTarget target) {
        return target.type() == CombatEntityType.PLAYER && target.alive() && !target.friend()
                && !target.suspectedBot() && target.lineOfSight() && target.distance() <= targetRange.value();
    }

    public record Action(Stage stage, CombatTarget target, int comboIndex, boolean attack,
                         double orbitRadius, double safetyHeight) {
    }
}
