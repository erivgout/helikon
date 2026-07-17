package dev.helikon.client.module.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.PlayerModelPart;

import java.util.Objects;

/** Maps the pure skin-layer port to the verified Minecraft 26.2 options API. */
public final class MinecraftSkinLayerAccess implements SkinLayerAccess {
    @Override
    public boolean isEnabled(SkinLayer layer) {
        return Minecraft.getInstance().options.isModelPartEnabled(toModelPart(layer));
    }

    @Override
    public void setEnabled(SkinLayer layer, boolean enabled) {
        Minecraft.getInstance().options.setModelPart(toModelPart(layer), enabled);
    }

    private static PlayerModelPart toModelPart(SkinLayer layer) {
        return switch (Objects.requireNonNull(layer, "layer")) {
            case CAPE -> PlayerModelPart.CAPE;
            case JACKET -> PlayerModelPart.JACKET;
            case LEFT_SLEEVE -> PlayerModelPart.LEFT_SLEEVE;
            case RIGHT_SLEEVE -> PlayerModelPart.RIGHT_SLEEVE;
            case LEFT_PANTS_LEG -> PlayerModelPart.LEFT_PANTS_LEG;
            case RIGHT_PANTS_LEG -> PlayerModelPart.RIGHT_PANTS_LEG;
            case HAT -> PlayerModelPart.HAT;
        };
    }
}
