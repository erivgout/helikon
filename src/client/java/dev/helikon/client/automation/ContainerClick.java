package dev.helikon.client.automation;

/** Minecraft-free description of one ordinary vanilla container interaction. */
public record ContainerClick(int slot, int button, Type type) {
    public ContainerClick {
        if (slot < 0) {
            throw new IllegalArgumentException("slot must be non-negative");
        }
        if (button < 0 || button > 1) {
            throw new IllegalArgumentException("button must be 0 or 1");
        }
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
    }

    public enum Type {
        PICKUP,
        QUICK_MOVE,
        THROW
    }
}
