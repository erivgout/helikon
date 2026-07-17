package dev.helikon.client.module.combat;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;
import dev.helikon.client.setting.BooleanSetting;
import dev.helikon.client.setting.NumberSetting;

import java.util.Objects;

/**
 * Repeats ordinary right-click interactions at a configurable local rate.
 *
 * <p>The decision logic is Minecraft-free: {@link #decide(long, Context)} maps
 * a local snapshot to the interaction category the narrow adapter should perform
 * this tick. The connected server remains authoritative and may reject, ignore,
 * or rate-limit any interaction Helikon requests.
 */
public final class RightClicker extends Module {
    /** The kind of thing the local crosshair currently rests on. */
    public enum HitKind {
        MISS,
        BLOCK,
        ENTITY
    }

    /** The single ordinary interaction the adapter should perform this tick, if any. */
    public enum Decision {
        NONE,
        USE_ON_BLOCK,
        INTERACT_ENTITY,
        USE_ITEM
    }

    /** Minecraft-free local facts needed before an ordinary right-click may be requested. */
    public record Context(boolean playerAvailable, boolean screenOpen, boolean useKeyHeld, boolean usingItem,
                          boolean hasHeldItem, HitKind hitKind, boolean hitIsFriend) {
        public Context {
            Objects.requireNonNull(hitKind, "hitKind");
        }
    }

    private final NumberSetting clicksPerSecond;
    private final BooleanSetting requireUseKeyHeld;
    private final BooleanSetting requireItem;
    private final BooleanSetting blocks;
    private final BooleanSetting entities;
    private final BooleanSetting items;
    private final BooleanSetting excludeFriends;
    private long nextClickTick = Long.MIN_VALUE;

    public RightClicker() {
        super("right_clicker", "RightClicker",
                "Repeats ordinary right-click interactions at a configurable local rate.",
                ModuleCategory.COMBAT, false, Keybind.unbound());
        clicksPerSecond = addSetting(new NumberSetting("clicks_per_second", "Clicks per second",
                "Target ordinary right-click interactions per second; client ticks cap the effective rate near 20.",
                8.0D, 1.0D, 20.0D));
        requireUseKeyHeld = addSetting(new BooleanSetting("require_use_key_held", "Require use key held",
                "Only interact while the physical Use button is held.", true));
        requireItem = addSetting(new BooleanSetting("require_item", "Require held item",
                "Only interact while a non-empty item is held in either hand.", false));
        blocks = addSetting(new BooleanSetting("blocks", "Blocks",
                "Use the held item on the targeted block (place, activate, till, etc.).", true));
        entities = addSetting(new BooleanSetting("entities", "Entities",
                "Interact with the targeted entity (trade, feed, mount, etc.).", true));
        items = addSetting(new BooleanSetting("items", "Items",
                "Use the held item toward air (throw, eat, draw, bucket, etc.).", true));
        excludeFriends = addSetting(new BooleanSetting("exclude_friends", "Exclude friends",
                "Never interact with a locally listed friend entity.", true));
    }

    /**
     * Returns the single ordinary interaction the narrow adapter may perform this
     * tick. The rate limiter only advances when a real interaction is chosen, so a
     * blank tick (wrong target for the current settings) does not consume the cooldown.
     */
    public Decision decide(long clientTick, Context context) {
        if (clientTick < 0L) {
            throw new IllegalArgumentException("clientTick must not be negative");
        }
        Context current = Objects.requireNonNull(context, "context");
        if (!isEnabled() || !current.playerAvailable() || current.screenOpen() || current.usingItem()) {
            return Decision.NONE;
        }
        if (requireUseKeyHeld.value() && !current.useKeyHeld()) {
            return Decision.NONE;
        }
        if (requireItem.value() && !current.hasHeldItem()) {
            return Decision.NONE;
        }
        if (clientTick < nextClickTick) {
            return Decision.NONE;
        }

        Decision decision = decisionFor(current);
        if (decision == Decision.NONE) {
            return Decision.NONE;
        }

        long interval = intervalTicks();
        nextClickTick = clientTick > Long.MAX_VALUE - interval ? Long.MAX_VALUE : clientTick + interval;
        return decision;
    }

    /** Minimum local ticks between interactions derived from the configured click rate. */
    public long intervalTicks() {
        return Math.max(1L, Math.round(20.0D / clicksPerSecond.value()));
    }

    private Decision decisionFor(Context context) {
        return switch (context.hitKind()) {
            case BLOCK -> blocks.value() ? Decision.USE_ON_BLOCK : Decision.NONE;
            case ENTITY -> entities.value() && !(excludeFriends.value() && context.hitIsFriend())
                    ? Decision.INTERACT_ENTITY : Decision.NONE;
            case MISS -> items.value() && context.hasHeldItem() ? Decision.USE_ITEM : Decision.NONE;
        };
    }

    @Override
    protected void onEnable() {
        nextClickTick = Long.MIN_VALUE;
    }

    @Override
    protected void onDisable() {
        nextClickTick = Long.MIN_VALUE;
    }
}
