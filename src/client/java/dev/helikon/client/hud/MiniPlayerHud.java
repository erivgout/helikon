package dev.helikon.client.hud;

import dev.helikon.client.module.render.MiniPlayer;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.MiniPlayerLayout;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.Objects;

/** Thin 26.2 HUD adapter for a local player render-state miniature. */
public final class MiniPlayerHud implements HudElement {
    private static final int OUTLINE_COLOR = 0xFF8A919E;
    private static final float CAMERA_PITCH_RADIANS = (float) Math.toRadians(-15.0D);

    private final MiniPlayer module;
    private final PanicState panicState;
    private final HudLayout layout;

    public MiniPlayerHud(MiniPlayer module, PanicState panicState) {
        this(module, panicState, new HudLayout());
    }

    public MiniPlayerHud(MiniPlayer module, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || !layout.element(HudElementId.MINI_PLAYER).enabled() || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        HudBounds rawBounds = MiniPlayerLayout.bounds();
        HudBounds bounds = layout.element(HudElementId.MINI_PLAYER).bounds(graphics.guiWidth(), graphics.guiHeight(),
                rawBounds.width(), rawBounds.height());
        graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(),
                module.backgroundColor());
        graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), OUTLINE_COLOR);

        EntityRenderState state = client.getEntityRenderDispatcher().extractEntity(client.player,
                deltaTracker.getGameTimeDeltaPartialTick(false));
        if (!(state instanceof LivingEntityRenderState livingState)) {
            return;
        }
        if (!module.armorEnabled() && state instanceof HumanoidRenderState humanoidState) {
            humanoidState.headEquipment = ItemStack.EMPTY;
            humanoidState.chestEquipment = ItemStack.EMPTY;
            humanoidState.legsEquipment = ItemStack.EMPTY;
            humanoidState.feetEquipment = ItemStack.EMPTY;
            humanoidState.headItem.clear();
        }
        normalizeEntityScale(livingState);
        Quaternionf orientation = new Quaternionf().rotateZ((float) Math.PI)
                .rotateY((float) Math.toRadians(module.rotation()));
        Quaternionf cameraOrientation = new Quaternionf().rotateX(CAMERA_PITCH_RADIANS);
        Vector3f offset = new Vector3f(0.0F, state.boundingBoxHeight / 2.0F, 0.0F);
        graphics.entity(state, MiniPlayerLayout.entitySize(module.scale()), offset, orientation, cameraOrientation,
                bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static void normalizeEntityScale(LivingEntityRenderState state) {
        if (state.scale <= 0.0F || !Float.isFinite(state.scale)) {
            return;
        }
        state.boundingBoxWidth /= state.scale;
        state.boundingBoxHeight /= state.scale;
        state.scale = 1.0F;
    }
}
