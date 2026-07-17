package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;

/** Local policy for selectively declining vanilla client movement slowdown calculations. */
public final class NoSlow extends Module {
    public enum UseKind {
        EATING,
        BLOCKING,
        BOW,
        OTHER
    }

    private final BooleanSetting eating;
    private final BooleanSetting blocking;
    private final BooleanSetting bowUse;
    private final BooleanSetting sneaking;
    private final BooleanSetting soulSand;
    private final BooleanSetting honey;
    private final BooleanSetting cobwebs;

    public NoSlow() {
        super("no_slow", "NoSlow", "Selectively removes local vanilla movement slowdowns without bypass modes.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        eating = addSetting(new BooleanSetting("eating", "Eating", "Ignore local food/drink use slowdown.", true));
        blocking = addSetting(new BooleanSetting("blocking", "Blocking", "Ignore local blocking use slowdown.", true));
        bowUse = addSetting(new BooleanSetting("bow_use", "Bow use", "Ignore local bow/crossbow use slowdown.", true));
        sneaking = addSetting(new BooleanSetting("sneaking", "Sneaking", "Ignore local sneak movement slowdown.", false));
        soulSand = addSetting(new BooleanSetting("soul_sand", "Soul sand", "Ignore local soul-sand slowdown.", false));
        honey = addSetting(new BooleanSetting("honey", "Honey", "Ignore local honey-block slowdown.", false));
        cobwebs = addSetting(new BooleanSetting("cobwebs", "Cobwebs", "Ignore local cobweb slowdown where feasible.", false));
    }

    public boolean ignoresUseSlowdown(UseKind kind) {
        return isEnabled() && switch (kind) {
            case EATING -> eating.value();
            case BLOCKING -> blocking.value();
            case BOW -> bowUse.value();
            case OTHER -> false;
        };
    }

    public boolean ignoresSneakSlowdown(boolean currentlySneaking) {
        return isEnabled() && currentlySneaking && sneaking.value();
    }

    public boolean ignoresSoulSand() {
        return isEnabled() && soulSand.value();
    }

    public boolean ignoresHoney() {
        return isEnabled() && honey.value();
    }

    public boolean ignoresCobwebs() {
        return isEnabled() && cobwebs.value();
    }
}
