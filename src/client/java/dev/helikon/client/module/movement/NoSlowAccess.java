package dev.helikon.client.module.movement;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.block.Blocks;

import java.util.Objects;

/** Narrow Minecraft mapping for NoSlow's tested category policy. */
public final class NoSlowAccess {
    private static volatile NoSlow module;

    private NoSlowAccess() {
    }

    public static void install(NoSlow noSlow) {
        module = Objects.requireNonNull(noSlow, "noSlow");
    }

    public static boolean ignoreUse(ItemUseAnimation animation) {
        NoSlow current = module;
        return current != null && current.ignoresUseSlowdown(switch (animation) {
            case EAT, DRINK -> NoSlow.UseKind.EATING;
            case BLOCK -> NoSlow.UseKind.BLOCKING;
            case BOW, CROSSBOW -> NoSlow.UseKind.BOW;
            default -> NoSlow.UseKind.OTHER;
        });
    }

    public static boolean ignoreSneak(LocalPlayer player) {
        NoSlow current = module;
        return current != null && current.ignoresSneakSlowdown(player.isShiftKeyDown());
    }

    public static boolean ignoreBlockFactor(LocalPlayer player) {
        NoSlow current = module;
        if (current == null) {
            return false;
        }
        var state = player.getBlockStateOn();
        return (state.is(Blocks.SOUL_SAND) && current.ignoresSoulSand())
                || (state.is(Blocks.HONEY_BLOCK) && current.ignoresHoney());
    }

    public static boolean ignoreCobweb() {
        NoSlow current = module;
        return current != null && current.ignoresCobwebs();
    }
}
