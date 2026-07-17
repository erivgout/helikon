package dev.helikon.client.module.miscellaneous;

/** Narrow port for the local, session-only player model-layer option set. */
public interface SkinLayerAccess {
    boolean isEnabled(SkinLayer layer);

    void setEnabled(SkinLayer layer, boolean enabled);
}
