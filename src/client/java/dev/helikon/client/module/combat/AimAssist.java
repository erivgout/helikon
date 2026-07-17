package dev.helikon.client.module.combat;

import dev.helikon.client.combat.CombatAim;
import dev.helikon.client.combat.CombatTarget;
import dev.helikon.client.combat.CombatTargetFilter;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.List;
import java.util.Optional;

/**
 * Pulls the local crosshair toward one eligible melee-range target at a bounded rate. Unlike BowAimAssist it is not
 * limited to a held bow. It only nudges this client's view rotation; the server remains authoritative and may reject,
 * correct, or rubber-band any resulting interaction.
 */
public final class AimAssist extends Module {
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;
    private final NumberSetting range;
    private final NumberSetting fieldOfView;
    private final NumberSetting rotationSpeed;
    private final EnumSetting<CombatTargetFilter.Priority> priority;
    private final BooleanSetting requireWeapon;
    private final BooleanSetting requireAttackKey;
    private String markerTargetId;

    public AimAssist() {
        super("aim_assist", "AimAssist",
                "Nudges the local crosshair toward one eligible target within melee range at a bounded rate.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        players = addSetting(new BooleanSetting("players", "Players", "Allow non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Allow hostile mobs.", true));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Allow passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never aim at locally listed friends.", true));
        range = addSetting(new NumberSetting("range", "Range", "Maximum locally considered target distance.",
                4.5D, 1.0D, 6.0D));
        fieldOfView = addSetting(new NumberSetting("field_of_view", "Field of view", "Maximum target angle from view.",
                60.0D, 5.0D, 180.0D));
        rotationSpeed = addSetting(new NumberSetting("rotation_speed", "Rotation speed",
                "Maximum local yaw or pitch adjustment per client tick.", 6.0D, 0.25D, 30.0D));
        priority = addSetting(new EnumSetting<>("priority", "Priority",
                "Order eligible targets by distance, health, or angle.",
                CombatTargetFilter.Priority.class, CombatTargetFilter.Priority.ANGLE));
        requireWeapon = addSetting(new BooleanSetting("require_weapon", "Require weapon",
                "Only assist while holding a conventional melee weapon.", true));
        requireAttackKey = addSetting(new BooleanSetting("require_attack_key", "Require attack key",
                "Only assist while the Attack key is held.", false));
    }

    /**
     * Returns a bounded local view adjustment toward the highest-priority eligible target, or empty when none applies.
     * It never constructs a rotation packet; the caller applies the result to the ordinary local view only.
     */
    public Optional<CombatAim.Rotation> nextRotation(List<CombatTarget> candidates, CombatAim.Rotation current) {
        if (!isEnabled() || current == null || candidates == null) {
            markerTargetId = null;
            return Optional.empty();
        }
        Optional<CombatTarget> target = CombatTargetFilter.ordered(candidates, targetOptions(), priority.value())
                .stream().findFirst();
        if (target.isEmpty()) {
            markerTargetId = null;
            return Optional.empty();
        }
        markerTargetId = target.get().id();
        CombatAim.Rotation desired = CombatAim.predictedRotation(target.get(), 1.0D, 0.0D, false);
        return Optional.of(CombatAim.limit(current, desired, rotationSpeed.value()));
    }

    public boolean requireWeapon() {
        return requireWeapon.value();
    }

    public boolean requireAttackKey() {
        return requireAttackKey.value();
    }

    public Optional<String> markerTargetId() {
        return Optional.ofNullable(markerTargetId);
    }

    /** Clears the stale local marker when the player leaves the active world or the module is gated off. */
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
