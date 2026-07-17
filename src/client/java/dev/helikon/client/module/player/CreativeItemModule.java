package dev.helikon.client.module.player;

import dev.helikon.client.input.Keybind;
import dev.helikon.client.module.Module;
import dev.helikon.client.module.ModuleCategory;

import java.util.Optional;

/** Shared one-shot policy for bounded, well-formed Creative inventory item requests. */
public abstract class CreativeItemModule extends Module {
    private boolean delivered;

    protected CreativeItemModule(String id, String name, String description) {
        super(id, name, description, ModuleCategory.PLAYER, false, Keybind.unbound());
    }

    public Optional<Request> nextRequest(boolean creative, boolean screenOpen) {
        if (!isEnabled() || delivered || !creative || screenOpen) {
            return Optional.empty();
        }
        return Optional.of(request());
    }

    protected abstract Request request();

    public void markDelivered() {
        delivered = true;
    }

    public void onContextLost() {
        delivered = false;
    }

    @Override
    protected void onDisable() {
        delivered = false;
    }

    public record Request(Kind kind, String itemId, int count, String customName) {
        public Request {
            if (kind == null || itemId == null || itemId.isBlank() || count < 1 || count > 64
                    || customName == null || customName.length() > 64) {
                throw new IllegalArgumentException("Creative item request is invalid");
            }
        }
    }

    public enum Kind {
        ITEM,
        KILL_POTION,
        TROLL_POTION,
        COMMAND_BLOCK
    }
}
