package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.EnumSetting;
import dev.helikon.client.setting.StringListSetting;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared, Minecraft-free player-target policy consumed by the combat targeting
 * pipeline. When enabled it removes disallowed players from the candidate set
 * before any targeting module (TriggerBot, KillAura, CriticalAssist,
 * BowAimAssist, TargetHUD) sees them, so a single local list controls which
 * players those modules may target. It is honest about server authority: it
 * only narrows the local candidates and never sends packets or claims a
 * server-side result.
 */
public final class TargetFilter extends Module {
    /** How the configured player-name list is interpreted. */
    public enum Mode {
        /** Only listed players may be targeted; every other player is excluded. */
        ALLOWLIST,
        /** Listed players are excluded; every other player may be targeted. */
        BLOCKLIST
    }

    private final EnumSetting<Mode> mode;
    private final StringListSetting players;
    private final BooleanSetting excludeFriends;

    public TargetFilter() {
        super("target_filter", "TargetFilter",
                "Shared local filter controlling which players other combat modules may target.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        mode = addSetting(new EnumSetting<>("mode", "Mode",
                "Allowlist targets only listed players; Blocklist targets everyone except listed players.",
                Mode.class, Mode.BLOCKLIST));
        players = addSetting(new StringListSetting("players", "Players",
                "Case-insensitive player names the filter applies to.", List.of(), 128, 32, false));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never allow locally listed friends as targets while the filter is enabled.", true));
    }

    /**
     * Returns whether the given player may be targeted under the current settings.
     * A disabled filter never restricts targeting, so it returns {@code true}.
     * A blank or {@code null} name is treated as an unknown player and is matched
     * against the configured list like any other name. Non-player entities are
     * outside this filter's scope and callers should not pass them here.
     */
    public boolean allowsPlayer(String playerName, boolean friend) {
        if (!isEnabled()) {
            return true;
        }
        if (excludeFriends.value() && friend) {
            return false;
        }
        String normalized = playerName == null ? "" : playerName.trim().toLowerCase(Locale.ROOT);
        boolean listed = names().contains(normalized);
        return switch (mode.value()) {
            case ALLOWLIST -> listed;
            case BLOCKLIST -> !listed;
        };
    }

    private Set<String> names() {
        return players.value().stream()
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .filter(name -> !name.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
