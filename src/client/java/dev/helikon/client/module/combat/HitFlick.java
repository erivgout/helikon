package dev.helikon.client.module.combat;

import dev.helikon.client.combat.HitFlickPolicy;
import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Optional;

/**
 * Flicks the reported aim off a melee target for the attack tick so the server steers the sprint and
 * knockback-enchantment bonus, then restores the real view. It only decides the flicked yaw; the thin
 * {@link MinecraftHitFlickAccess} adapter sends the ordinary rotation packets. Knockback is computed
 * server-side, so the server remains authoritative and may reject or correct the outcome.
 */
public final class HitFlick extends Module {
    private final BooleanSetting excludeFriends;
    private final EnumSetting<HitFlickPolicy.Mode> mode;
    private final NumberSetting sideAngle;

    public HitFlick() {
        super("hit_flick", "HitFlick",
                "Flicks aim off a target at attack time to steer server knockback; the server may reject or correct it.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never flick when attacking a locally listed friend.", true));
        mode = addSetting(new EnumSetting<>("mode", "Mode",
                "Reverse pulls the target toward you; Left and Right steer it to the side.",
                HitFlickPolicy.Mode.class, HitFlickPolicy.Mode.REVERSE));
        sideAngle = addSetting(new NumberSetting("side_angle", "Side angle",
                "Yaw offset in degrees applied for the Left and Right modes.", 30.0D, 5.0D, 180.0D));
    }

    public boolean excludeFriends() {
        return excludeFriends.value();
    }

    /**
     * Returns the flicked yaw for a target direction, or empty when the module is disabled.
     *
     * @param yawToTarget the yaw (degrees) pointing from the attacker toward the target
     */
    public Optional<Float> flickYaw(float yawToTarget) {
        if (!isEnabled()) {
            return Optional.empty();
        }
        return Optional.of(HitFlickPolicy.flickedYaw(yawToTarget, mode.value(), sideAngle.value()));
    }
}
