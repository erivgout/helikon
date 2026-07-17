package dev.helikon.client.render;

import com.mojang.blaze3d.platform.NativeImage;
import dev.helikon.client.module.ModuleRegistry;
import dev.helikon.client.module.miscellaneous.LocalCape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Objects;

/** Thin client renderer that substitutes a generated cape in the local avatar's transient render state. */
public final class LocalCapeRenderer {
    private static final Identifier CAPE_TEXTURE_ID = Identifier.fromNamespaceAndPath("helikon", "dynamic/local_cape");
    private static final ClientAsset.Texture CAPE_TEXTURE = new ClientAsset.Texture() {
        @Override
        public Identifier id() {
            return CAPE_TEXTURE_ID;
        }

        @Override
        public Identifier texturePath() {
            return CAPE_TEXTURE_ID;
        }
    };

    private static volatile ModuleRegistry modules;
    private static volatile LocalCape localCape;
    private static DynamicTexture dynamicTexture;
    private static int uploadedPrimary = Integer.MIN_VALUE;
    private static int uploadedAccent = Integer.MIN_VALUE;

    private LocalCapeRenderer() {
    }

    /** Installs the sole local module instance used by the avatar-render mixin. */
    public static void install(ModuleRegistry moduleRegistry, LocalCape localCapeModule) {
        modules = Objects.requireNonNull(moduleRegistry, "moduleRegistry");
        localCape = Objects.requireNonNull(localCapeModule, "localCapeModule");
    }

    /** Applies the procedural texture only to the current client's own transient avatar render state. */
    public static void apply(Avatar avatar, AvatarRenderState renderState) {
        LocalCape module = localCape;
        ModuleRegistry registry = modules;
        Minecraft client = Minecraft.getInstance();
        if (registry == null || module == null || !module.isEnabled() || client.player == null || avatar != client.player
                || renderState.skin == null) {
            return;
        }

        registry.runGuarded(module, "render", () -> applyCape(client, module, renderState));
    }

    private static void applyCape(Minecraft client, LocalCape module, AvatarRenderState renderState) {
        ensureTexture(client, module.primaryColor(), module.accentColor());
        PlayerSkin original = renderState.skin;
        renderState.skin = new PlayerSkin(original.body(), CAPE_TEXTURE, original.elytra(), original.model(), original.secure());
        renderState.showCape = true;
    }

    private static void ensureTexture(Minecraft client, int primary, int accent) {
        if (dynamicTexture == null) {
            dynamicTexture = new DynamicTexture("helikon_local_cape", LocalCapeTexturePattern.WIDTH,
                    LocalCapeTexturePattern.HEIGHT, true);
            client.getTextureManager().register(CAPE_TEXTURE_ID, dynamicTexture);
        }
        if (uploadedPrimary == primary && uploadedAccent == accent) {
            return;
        }

        NativeImage pixels = dynamicTexture.getPixels();
        if (pixels == null) {
            throw new IllegalStateException("Helikon local cape texture has no pixel buffer");
        }
        for (int y = 0; y < LocalCapeTexturePattern.HEIGHT; y++) {
            for (int x = 0; x < LocalCapeTexturePattern.WIDTH; x++) {
                pixels.setPixelABGR(x, y, LocalCapeTexturePattern.toAbgr(
                        LocalCapeTexturePattern.argbAt(x, y, primary, accent)));
            }
        }
        dynamicTexture.upload();
        uploadedPrimary = primary;
        uploadedAccent = accent;
    }
}
