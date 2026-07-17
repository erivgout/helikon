package dev.helikon.client.hud;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.module.render.Radar;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.render.RadarProjection;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.Objects;

/** Thin HUD adapter for Radar's Minecraft-free projection and local entity filters. */
public final class RadarHud implements HudElement {
    private static final int RADIUS = 38;
    private static final int PLAYER_COLOR = 0xFFFFFFFF;

    private final Radar module;
    private final FriendManager friends;
    private final PanicState panicState;
    private final HudLayout layout;

    public RadarHud(Radar module, FriendManager friends, PanicState panicState) {
        this(module, friends, panicState, new HudLayout());
    }

    public RadarHud(Radar module, FriendManager friends, PanicState panicState, HudLayout layout) {
        this.module = Objects.requireNonNull(module, "module");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || !layout.element(HudElementId.RADAR).enabled() || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        HudElementPlacement placement = layout.element(HudElementId.RADAR);
        HudPresentation.Frame frame = HudPresentation.beginFrame(graphics, placement, RADIUS * 2, RADIUS * 2);
        HudBounds localBounds = new HudBounds(frame.contentX(), frame.contentY(), RADIUS * 2, RADIUS * 2);
        int centerX = frame.contentX() + RADIUS;
        int centerY = frame.contentY() + RADIUS;
        drawBackground(graphics, localBounds, centerX, centerY, placement);
        EntityRenderFilter.Options options = module.options();
        int rendered = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, entity == client.player,
                    entity.position().distanceToSqr(client.player.position()))) {
                continue;
            }
            RadarProjection.Point point = RadarProjection.project(entity.getX() - client.player.getX(),
                    entity.getZ() - client.player.getZ(), client.player.getYRot(), module.zoom(), RADIUS,
                    module.rotate(), module.shape());
            if (!point.visible()) {
                continue;
            }
            int x = centerX + (int) Math.round(point.x());
            int y = centerY - (int) Math.round(point.y());
            graphics.fill(x - 1, y - 1, x + 2, y + 2, module.color(friend));
            if (++rendered >= module.maximumEntities()) {
                break;
            }
        }
        graphics.fill(centerX - 1, centerY - 1, centerX + 2, centerY + 2, PLAYER_COLOR);
        HudPresentation.endFrame(graphics);
    }

    private void drawBackground(GuiGraphicsExtractor graphics, HudBounds bounds, int centerX, int centerY,
                                HudElementPlacement placement) {
        if (!placement.background()) {
            return;
        }
        if (module.shape() == RadarProjection.Shape.SQUARE) {
            graphics.fill(bounds.x(), bounds.y(), bounds.x() + bounds.width(), bounds.y() + bounds.height(),
                    module.backgroundColor());
            graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), HudPresentation.color(placement));
            return;
        }
        for (int offsetY = -RADIUS; offsetY <= RADIUS; offsetY++) {
            int halfWidth = (int) Math.floor(Math.sqrt(RADIUS * RADIUS - offsetY * offsetY));
            graphics.fill(centerX - halfWidth, centerY + offsetY, centerX + halfWidth + 1,
                    centerY + offsetY + 1, module.backgroundColor());
        }
    }

    private boolean isFriend(Entity entity) {
        return entity instanceof Player player && friends.contains(player.getGameProfile().name());
    }

    private static EntityRenderFilter.EntityType entityType(Entity entity) {
        if (entity instanceof Player) {
            return EntityRenderFilter.EntityType.PLAYER;
        }
        if (entity instanceof Monster) {
            return EntityRenderFilter.EntityType.HOSTILE;
        }
        if (entity instanceof ItemEntity) {
            return EntityRenderFilter.EntityType.ITEM;
        }
        if (entity instanceof Projectile) {
            return EntityRenderFilter.EntityType.PROJECTILE;
        }
        if (entity instanceof LivingEntity) {
            return EntityRenderFilter.EntityType.PASSIVE;
        }
        return EntityRenderFilter.EntityType.OTHER;
    }
}
