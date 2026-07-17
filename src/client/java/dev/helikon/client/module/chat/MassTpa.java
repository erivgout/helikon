package dev.helikon.client.module.chat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.IntegerSetting;
import dev.helikon.client.setting.StringSetting;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Sends a bounded sequence of ordinary teleport-request commands to locally listed players. */
public final class MassTpa extends Module {
    private final StringSetting command;
    private final IntegerSetting delayTicks;
    private final IntegerSetting maximumPlayers;
    private final BooleanSetting includeFriends;
    private final ArrayDeque<String> pending = new ArrayDeque<>();
    private final Set<String> sent = new LinkedHashSet<>();
    private long lastSendTick = Long.MIN_VALUE;

    public MassTpa() {
        super("mass_tpa", "MassTPA",
                "Sends bounded ordinary teleport-request commands to players already listed by the server.",
                ModuleCategory.CHAT, false, Keybind.unbound());
        command = addSetting(new StringSetting("command", "Command",
                "Server command name without a slash.", "tpa", 32, false));
        delayTicks = addSetting(new IntegerSetting("delay_ticks", "Delay ticks",
                "Minimum delay between requests.", 60, 40, 400));
        maximumPlayers = addSetting(new IntegerSetting("maximum_players", "Maximum players",
                "Maximum requests per enabled session.", 12, 1, 32));
        includeFriends = addSetting(new BooleanSetting("include_friends", "Include friends",
                "Also send requests to locally saved friends.", false));
    }

    public Optional<String> nextCommand(long tick, List<Candidate> candidates) {
        if (tick < 0L || candidates == null) {
            throw new IllegalArgumentException("MassTPA inputs are invalid");
        }
        if (!isEnabled() || (lastSendTick != Long.MIN_VALUE && tick - lastSendTick < delayTicks.value())) {
            return Optional.empty();
        }
        for (Candidate candidate : candidates) {
            if (pending.size() + sent.size() >= maximumPlayers.value()) {
                break;
            }
            if ((!candidate.friend() || includeFriends.value()) && validName(candidate.name())
                    && !sent.contains(candidate.name()) && !pending.contains(candidate.name())) {
                pending.addLast(candidate.name());
            }
        }
        String name = pending.pollFirst();
        if (name == null) {
            return Optional.empty();
        }
        sent.add(name);
        lastSendTick = tick;
        return Optional.of(command.value() + " " + name);
    }

    public void onContextLost() {
        pending.clear();
        sent.clear();
        lastSendTick = Long.MIN_VALUE;
    }

    private static boolean validName(String name) {
        return name != null && name.matches("[A-Za-z0-9_]{1,16}");
    }

    @Override
    protected void onDisable() {
        onContextLost();
    }

    public record Candidate(String name, boolean friend) {
    }
}
