package dev.helikon.client.module.player;

import com.mojang.blaze3d.platform.InputConstants;
import dev.helikon.client.mixin.KeyMappingAccessor;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/** Minecraft-only port for AutoEat's normal local Use-key ownership. */
public final class MinecraftUseKeyAccess implements AutoEat.UseKeyAccess {
    @Override
    public boolean isPhysicallyDown() {
        Minecraft client = Minecraft.getInstance();
        InputConstants.Key key = ((KeyMappingAccessor) client.options.keyUse).helikon$getKey();
        return switch (key.getType()) {
            case KEYSYM -> InputConstants.isKeyDown(client.getWindow(), key.getValue());
            case MOUSE -> GLFW.glfwGetMouseButton(client.getWindow().handle(), key.getValue()) == GLFW.GLFW_PRESS;
            case SCANCODE -> isScancodeDown(client, key.getValue());
        };
    }

    @Override
    public void setDown(boolean value) {
        Minecraft.getInstance().options.keyUse.setDown(value);
    }

    private static boolean isScancodeDown(Minecraft client, int scanCode) {
        long window = client.getWindow().handle();
        for (int keyCode = GLFW.GLFW_KEY_SPACE; keyCode <= GLFW.GLFW_KEY_LAST; keyCode++) {
            if (GLFW.glfwGetKeyScancode(keyCode) == scanCode
                    && GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS) {
                return true;
            }
        }
        return false;
    }
}
