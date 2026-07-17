package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Local policy for declining vanilla entity-collision push on the player. */
public final class AntiEntityPush extends Module {
    private final BooleanSetting onlyWhileSneaking;

    public AntiEntityPush() {
        super("anti_entity_push", "AntiEntityPush",
                "Prevents ordinary entity collisions from pushing the local player.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        onlyWhileSneaking = addSetting(new BooleanSetting(
                "only_while_sneaking",
                "Only while sneaking",
                "Prevent collision push only while the local player is sneaking.",
                false
        ));
    }

    /**
     * Returns whether the local player's vanilla collision-push response should be suppressed.
     * Damage knockback and other velocity sources do not use this decision.
     */
    public boolean preventsCollisionPush(boolean sneaking) {
        return isEnabled() && (!onlyWhileSneaking.value() || sneaking);
    }
}
