package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/**
 * Delays incoming position updates for nearby eligible entities so they render at an
 * earlier position, letting the local player interact with the target where it recently
 * was. The server remains authoritative: it may reject, correct, rubber-band, or ignore
 * any resulting attack, and its lag-compensation window bounds how much delay is useful.
 *
 * <p>All decision logic here is Minecraft-free and unit tested. The {@link MinecraftBackTrackAccess}
 * bridge and a narrow {@code Connection} mixin apply the actual packet holding and release.
 */
public final class BackTrack extends Module {
    private final NumberSetting delayMillis;
    private final NumberSetting range;
    private final BooleanSetting players;
    private final BooleanSetting hostiles;
    private final BooleanSetting passive;
    private final BooleanSetting excludeFriends;

    public BackTrack() {
        super("backtrack", "BackTrack",
                "Delays nearby target position updates so they render where they recently were; the server stays authoritative.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        delayMillis = addSetting(new NumberSetting("delay_ms", "Delay",
                "Milliseconds to hold each eligible position update. Values beyond the server's lag-compensation window "
                        + "only add visible lag without landing hits.", 120.0D, 0.0D, 1000.0D));
        range = addSetting(new NumberSetting("range", "Range",
                "Only entities within this local distance are delayed.", 6.0D, 2.0D, 32.0D));
        players = addSetting(new BooleanSetting("players", "Players", "Delay non-friend players.", true));
        hostiles = addSetting(new BooleanSetting("hostiles", "Hostiles", "Delay hostile mobs.", false));
        passive = addSetting(new BooleanSetting("passive", "Passive", "Delay passive mobs.", false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never delay locally listed friends.", true));
    }

    /** The clamped hold duration, in milliseconds, applied to each eligible position update. */
    public long delayMillis() {
        return Math.round(delayMillis.value());
    }

    public double range() {
        return range.value();
    }

    /**
     * Decides whether a single locally observed entity's position updates should be delayed.
     * Pure and Minecraft-free so it can be exercised directly in tests.
     */
    public boolean shouldDelay(boolean isPlayer, boolean isHostile, boolean isFriend, double distance) {
        if (!isEnabled()) {
            return false;
        }
        if (isFriend && excludeFriends.value()) {
            return false;
        }
        boolean typeAllowed = (isPlayer && players.value())
                || (isHostile && hostiles.value())
                || (!isPlayer && !isHostile && passive.value());
        if (!typeAllowed) {
            return false;
        }
        return distance >= 0.0D && distance <= range.value();
    }
}
