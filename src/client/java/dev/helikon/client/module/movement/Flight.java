package dev.helikon.client.module.movement;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

/** Enables only Minecraft-permitted player flight and restores only state this module owns. */
public final class Flight extends Module {
    public record Abilities(boolean mayFly, boolean flying, float flyingSpeed) {
        public Abilities {
            if (!Float.isFinite(flyingSpeed) || flyingSpeed < 0.0F) {
                throw new IllegalArgumentException("flyingSpeed must be finite and non-negative");
            }
        }
    }

    public record Action(boolean setFlying, boolean flying, boolean setSpeed, float flyingSpeed) {
        private static final Action NONE = new Action(false, false, false, 0.0F);

        public static Action none() {
            return NONE;
        }
    }

    private final NumberSetting flyingSpeed;
    private final BooleanSetting freecamView;
    private final NumberSetting freecamSpeed;
    private boolean ownedFlying;
    private float previousSpeed = -1.0F;

    public Flight() {
        super("flight", "Flight", "Enables only Minecraft-permitted creative, spectator, or allowed flight.",
                ModuleCategory.MOVEMENT, false, Keybind.unbound());
        flyingSpeed = addSetting(new NumberSetting("flying_speed", "Flying speed",
                "Requested normal ability flight speed when Minecraft permits flight.", 0.05D, 0.01D, 0.20D));
        freecamView = addSetting(new BooleanSetting("freecam_view", "Freecam view",
                "Detach a local-only camera view without moving or sending input for the player.", false));
        freecamSpeed = addSetting(new NumberSetting("freecam_speed", "Freecam speed",
                "Local-only detached camera movement per client tick.", 0.15D, 0.02D, 0.50D));
    }

    public Action update(Abilities abilities) {
        if (abilities == null) {
            throw new IllegalArgumentException("abilities must not be null");
        }
        if (isEnabled() && freecamView.value()) {
            return restoreOwnedState(abilities);
        }
        if (isEnabled() && abilities.mayFly()) {
            if (previousSpeed < 0.0F) {
                previousSpeed = abilities.flyingSpeed();
            }
            boolean setFlying = !abilities.flying();
            ownedFlying |= setFlying;
            return new Action(setFlying, true, Math.abs(abilities.flyingSpeed() - flyingSpeed.value()) > 0.0001F,
                    flyingSpeed.value().floatValue());
        }
        if (!isEnabled()) {
            return restoreOwnedState(abilities);
        }
        clearOwnedState();
        return Action.none();
    }

    public boolean isFreecamView() {
        return isEnabled() && freecamView.value();
    }

    public double freecamSpeed() {
        return freecamSpeed.value();
    }

    @Override
    protected void onDisable() {
        // The next client tick observes and restores only ability state this module changed.
    }

    /** Clears transient ownership when the player/world is no longer a valid ability context. */
    public void onContextLost() {
        clearOwnedState();
    }

    private Action restoreOwnedState(Abilities abilities) {
        if (!ownedFlying && previousSpeed < 0.0F) {
            return Action.none();
        }
        Action action = new Action(ownedFlying && abilities.flying(), false, previousSpeed >= 0.0F, previousSpeed);
        clearOwnedState();
        return action;
    }

    private void clearOwnedState() {
        ownedFlying = false;
        previousSpeed = -1.0F;
    }
}
