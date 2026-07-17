package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/** Calculates a bounded local bow aim adjustment; it never fires the bow or sends rotation packets. */
public final class BowAimAssist extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final BooleanSetting prediction;
    private final NumberSetting projectileSpeed;
    private final NumberSetting gravity;
    private final NumberSetting adjustmentSpeed;
    private String markerTargetId;

    public BowAimAssist() {
        super("bow_aim_assist", "BowAimAssist", "Applies bounded local aim smoothing while the player holds a bow.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never aim at locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum locally considered target distance.",
                32.0D, 4.0D, 96.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", "Maximum target angle from view.",
                45.0D, 5.0D, 180.0D));
        prediction = addSetting(new BooleanSetting("prediction", "Projectile prediction",
                "Account locally for observed target velocity and ordinary bow gravity.", true));
        projectileSpeed = addSetting(new NumberSetting("projectile_speed", "Projectile speed",
                "Conservative local projectile-speed estimate in blocks per tick.", 3.0D, 0.5D, 5.0D));
        gravity = addSetting(new NumberSetting("gravity", "Gravity", "Conservative local gravity estimate per tick.",
                0.05D, 0.0D, 0.20D));
        adjustmentSpeed = addSetting(new NumberSetting("adjustment_speed", "Adjustment speed",
                "Maximum local yaw or pitch adjustment per client tick.", 4.0D, 0.25D, 15.0D));
    }

    public Optional<CombatAim.Rotation> nextRotation(List<CombatTarget> candidates, CombatAim.Rotation current) {
        if (!isEnabled() || current == null) {
            markerTargetId = null;
            return Optional.empty();
        }
        Optional<CombatTarget> target = CombatTargetFilter.ordered(candidates, targetOptions(),
                CombatTargetFilter.Priority.ANGLE).stream().findFirst();
        if (target.isEmpty()) {
            markerTargetId = null;
            return Optional.empty();
        }
        markerTargetId = target.get().id();
        CombatAim.Rotation desired = CombatAim.predictedRotation(target.get(), projectileSpeed.value(), gravity.value(),
                prediction.value());
        return Optional.of(CombatAim.limit(current, desired, adjustmentSpeed.value()));
    }

    public Optional<String> markerTargetId() {
        return Optional.ofNullable(markerTargetId);
    }

    /** Clears a stale local marker when the player leaves the active world. */
    public void onContextLost() {
        markerTargetId = null;
    }

    private CombatTargetFilter.Options targetOptions() {
        return new CombatTargetFilter.Options(players.value(), hostiles.value(), passive.value(), excludeFriends.value(),
                true, range.value(), fieldOfView.value(), true);
    }

    @Override
    protected void onDisable() {
        markerTargetId = null;
    }
}
