package dev.helikon.client.module.render;

/** The four locally rendered EntityESP presentations specified by the roadmap. */
public enum EntityEspMode {
    OUTLINE,
    BOX,
    GLOW,
    SHADER;

    public boolean usesNativeOutline() {
        return this == GLOW || this == SHADER;
    }

    public boolean usesShaderColor() {
        return this == SHADER;
    }
}
