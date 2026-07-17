package dev.helikon.client.module.movement;

/** The explicit local input policies offered by AutoSneak. */
public enum AutoSneakMode {
    /** Hold sneak continuously while the module is enabled. */
    TOGGLE,
    /** Hold sneak only while the module's configured key is physically down. */
    HOLD,
    /** Hold sneak while moving, so vanilla careful movement stops the player at ledges. */
    EDGE_ONLY
}
