package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;
import java.util.Optional;

/** Local reconnect countdown policy; the Minecraft adapter owns screens and ordinary connection setup. */
public final class AutoReconnect extends Module {
    private final NumberSetting countdownSeconds;
    private final NumberSetting maximumAttempts;
    private long dueTick = -1L;
    private long screenGraceDeadline = -1L;
    private int attempts;
    private String targetAddress;
    private boolean cancelled;

    public AutoReconnect() {
        super("auto_reconnect", "AutoReconnect", "Reconnects to a disconnected multiplayer server after a local countdown.",
                ModuleCategory.PLAYER, false, Keybind.unbound());
        countdownSeconds = addSetting(new NumberSetting("countdown_seconds", "Countdown", "Local seconds to wait before retrying.",
                5.0D, 1.0D, 60.0D));
        maximumAttempts = addSetting(new NumberSetting("maximum_attempts", "Maximum attempts",
                "Maximum automatic reconnect attempts for one disconnect.", 3.0D, 1.0D, 10.0D));
    }

    /** Starts a reconnect countdown only after the adapter has observed a multiplayer disconnect. */
    public void onDisconnected(long tick, String address) {
        if (!isEnabled()) {
            return;
        }
        String checkedAddress = Objects.requireNonNull(address, "address").trim();
        if (checkedAddress.isEmpty()) {
            throw new IllegalArgumentException("address must not be blank");
        }
        targetAddress = checkedAddress;
        attempts = 0;
        cancelled = false;
        dueTick = tick + countdownTicks();
        screenGraceDeadline = tick + 20L;
    }

    /** Makes one reconnect request only when the ordinary disconnect screen confirms this was not a manual leave. */
    public Optional<String> nextReconnect(long tick, boolean disconnectScreenVisible) {
        if (!isEnabled() || targetAddress == null || cancelled) {
            return Optional.empty();
        }
        if (!disconnectScreenVisible) {
            if (tick > screenGraceDeadline) {
                clear();
            }
            return Optional.empty();
        }
        if (tick < dueTick) {
            return Optional.empty();
        }
        if (attempts >= Math.round(maximumAttempts.value())) {
            clear();
            return Optional.empty();
        }
        attempts++;
        dueTick = Long.MAX_VALUE;
        return Optional.of(targetAddress);
    }

    /** Arms the next bounded countdown after a normal reconnect attempt returns to a disconnect screen. */
    public void onReconnectFailed(long tick) {
        if (targetAddress == null || cancelled || attempts >= Math.round(maximumAttempts.value())) {
            clear();
            return;
        }
        dueTick = tick + countdownTicks();
        screenGraceDeadline = tick + 20L;
    }

    public void cancel() {
        cancelled = true;
        dueTick = -1L;
    }

    public void onConnected() {
        clear();
    }

    public boolean isAwaitingDisconnectScreen() {
        return isEnabled() && targetAddress != null && !cancelled;
    }

    public int remainingSeconds(long tick) {
        if (dueTick < 0L || dueTick == Long.MAX_VALUE) {
            return 0;
        }
        return (int) Math.max(0L, (dueTick - tick + 19L) / 20L);
    }

    public int attempts() {
        return attempts;
    }

    private long countdownTicks() {
        return Math.round(countdownSeconds.value() * 20.0D);
    }

    @Override
    protected void onDisable() {
        clear();
    }

    private void clear() {
        dueTick = -1L;
        screenGraceDeadline = -1L;
        attempts = 0;
        targetAddress = null;
        cancelled = false;
    }
}
