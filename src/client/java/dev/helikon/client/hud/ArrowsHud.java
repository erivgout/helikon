package dev.helikon.client.hud;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.entity.MinecraftEntityClassification;
import dev.helikon.client.module.render.Arrows;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.ArrowGeometry;
import dev.helikon.client.render.ArrowProjection;
import dev.helikon.client.render.EntityRenderFilter;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/** Thin HUD adapter drawing Arrows' Minecraft-free indicators toward off-screen local enemies. */
public final class ArrowsHud implements HudElement {
    private final Arrows module;
    private final FriendManager friends;
    private final PanicState panicState;

    public ArrowsHud(Arrows module, FriendManager friends, PanicState panicState) {
        this.module = Objects.requireNonNull(module, "module");
        this.friends = Objects.requireNonNull(friends, "friends");
        this.panicState = Objects.requireNonNull(panicState, "panicState");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!module.isEnabled() || panicState.customHudHidden()) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null) {
            return;
        }
        EntityRenderFilter.Options options = module.options();
        Vec3 eye = client.player.getEyePosition();
        Vec3 origin = client.player.position();
        double centerX = graphics.guiWidth() / 2.0D;
        double centerY = graphics.guiHeight() / 2.0D;
        float yaw = client.player.getYRot();
        float pitch = client.player.getXRot();

        List<Target> targets = new ArrayList<>();
        for (Entity entity : client.level.entitiesForRendering()) {
            if (entity == client.player) {
                continue;
            }
            boolean friend = isFriend(entity);
            double distanceSquared = entity.position().distanceToSqr(origin);
            if (!EntityRenderFilter.shouldRender(options, entityType(entity), friend, false, distanceSquared)) {
                continue;
            }
            Vec3 targetEye = entity.getEyePosition();
            ArrowProjection.Result result = ArrowProjection.project(
                    targetEye.x - eye.x, targetEye.y - eye.y, targetEye.z - eye.z,
                    yaw, pitch, module.fieldOfView());
            if (!result.outside()) {
                continue;
            }
            targets.add(new Target(result, friend));
        }
        targets.sort(Comparator.comparingDouble(target -> target.result().distance()));

        int drawn = 0;
        int maximum = module.maximumTargets();
        for (Target target : targets) {
            if (drawn >= maximum) {
                break;
            }
            int color = module.color(target.friend());
            for (ArrowGeometry.Span span : ArrowGeometry.build(centerX, centerY, module.ringRadius(),
                    module.arrowLength(), module.arrowHalfWidth(),
                    target.result().directionX(), target.result().directionY())) {
                graphics.fill(span.xStart(), span.y(), span.xEnd(), span.y() + 1, color);
            }
            drawn++;
        }
    }

    private boolean isFriend(Entity entity) {
        return entity instanceof Player player && friends.contains(player.getGameProfile().name());
    }

    private static EntityRenderFilter.EntityType entityType(Entity entity) {
        if (entity instanceof Player) {
            return EntityRenderFilter.EntityType.PLAYER;
        }
        if (MinecraftEntityClassification.isHostile(entity)) {
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

    private record Target(ArrowProjection.Result result, boolean friend) {
    }
}
