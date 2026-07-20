package dev.helikon.client.hud;

import dev.helikon.client.friend.FriendManager;
import dev.helikon.client.entity.MinecraftEntityClassification;
import dev.helikon.client.module.render.Radar;
import dev.helikon.client.panic.PanicState;
import dev.helikon.client.render.EntityRenderFilter;
import dev.helikon.client.render.RadarMapColor;
import dev.helikon.client.render.RadarProjection;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Thin HUD adapter for Radar's Minecraft-free projection and local entity filters. */
public final class RadarHud implements HudElement {
    private static final int RADIUS = 38;
    private static final int MAP_CELL_SIZE = 1;
    private static final long MAP_REFRESH_TICKS = 10L;
    private static final int PLAYER_COLOR = 0xFF4FC3F7;
    private static final int PLAYER_OUTLINE_COLOR = 0xFFF4F7FA;
    private static final int MAP_GUIDE_COLOR = 0x502A313B;

    private final Radar module;
    private final FriendManager friends;
    private final PanicState panicState;
    private final HudLayout layout;
    private List<MapCell> mapCells = List.of();
    private long mapRefreshBucket = Long.MIN_VALUE;
    private int mapCenterX;
    private int mapCenterZ;
    private int mapYawBucket;
    private double mapZoom;
    private RadarProjection.Shape mapShape;

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
        if (module.minimap()) {
            drawMinimap(graphics, client, centerX, centerY);
        } else {
            mapCells = List.of();
            mapRefreshBucket = Long.MIN_VALUE;
        }
        drawMapGuides(graphics, client, localBounds, centerX, centerY, placement);
        EntityRenderFilter.Options options = module.options();
        int rendered = 0;
        for (Entity entity : client.level.entitiesForRendering()) {
            boolean friend = isFriend(entity);
            EntityRenderFilter.EntityType type = entityType(entity);
            if (!EntityRenderFilter.shouldRender(options, type, friend, entity == client.player,
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
            drawEntityMarker(graphics, x, y, module.color(type, friend), entity.getY() - client.player.getY());
            if (++rendered >= module.maximumEntities()) {
                break;
            }
        }
        drawPlayerMarker(graphics, centerX, centerY, client.player.getYRot(), module.rotate());
        HudPresentation.endFrame(graphics);
    }

    private void drawMinimap(GuiGraphicsExtractor graphics, Minecraft client, int centerX, int centerY) {
        long refreshBucket = client.level.getGameTime() / MAP_REFRESH_TICKS;
        int playerX = client.player.getBlockX();
        int playerZ = client.player.getBlockZ();
        int yawBucket = module.rotate() ? Math.round(client.player.getYRot() / 5.0F) : 0;
        if (refreshBucket != mapRefreshBucket || playerX != mapCenterX || playerZ != mapCenterZ
                || yawBucket != mapYawBucket || module.zoom() != mapZoom || module.shape() != mapShape) {
            mapCells = sampleMinimap(client, playerX, playerZ, yawBucket * 5.0D);
            mapRefreshBucket = refreshBucket;
            mapCenterX = playerX;
            mapCenterZ = playerZ;
            mapYawBucket = yawBucket;
            mapZoom = module.zoom();
            mapShape = module.shape();
        }
        for (MapCell cell : mapCells) {
            graphics.fill(centerX + cell.x(), centerY + cell.y(),
                    centerX + cell.x() + MAP_CELL_SIZE, centerY + cell.y() + MAP_CELL_SIZE, cell.color());
        }
    }

    private List<MapCell> sampleMinimap(Minecraft client, int centerBlockX, int centerBlockZ, double yawDegrees) {
        List<MapCell> cells = new ArrayList<>();
        double radians = Math.toRadians(yawDegrees);
        double cosine = Math.cos(radians);
        double sine = Math.sin(radians);
        int playerY = client.player.getBlockY();
        for (int screenY = -RADIUS; screenY < RADIUS; screenY += MAP_CELL_SIZE) {
            int previousHeight = Integer.MIN_VALUE;
            for (int screenX = -RADIUS; screenX < RADIUS; screenX += MAP_CELL_SIZE) {
                double sampleX = screenX + MAP_CELL_SIZE * 0.5D;
                double sampleY = screenY + MAP_CELL_SIZE * 0.5D;
                if (module.shape() == RadarProjection.Shape.CIRCLE
                        && sampleX * sampleX + sampleY * sampleY > RADIUS * RADIUS) {
                    continue;
                }
                double projectedX = sampleX / RADIUS * module.zoom();
                double projectedZ = -sampleY / RADIUS * module.zoom();
                double relativeX = module.rotate()
                        ? projectedX * cosine - projectedZ * sine : projectedX;
                double relativeZ = module.rotate()
                        ? projectedX * sine + projectedZ * cosine : projectedZ;
                int worldX = (int) Math.floor(centerBlockX + relativeX);
                int worldZ = (int) Math.floor(centerBlockZ + relativeZ);
                int surfaceY = client.level.getHeight(Heightmap.Types.MOTION_BLOCKING, worldX, worldZ) - 1;
                BlockPos position = new BlockPos(worldX, surfaceY, worldZ);
                if (!client.level.isLoaded(position)) {
                    continue;
                }
                BlockState state = client.level.getBlockState(position);
                MapColor mapColor = state.getMapColor(client.level, position);
                String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                int baseColor = mapColor == MapColor.NONE
                        ? RadarMapColor.forBlock(blockId, 0)
                        : mapColor.calculateARGBColor(MapColor.Brightness.NORMAL);
                int slope = previousHeight == Integer.MIN_VALUE ? 0 : surfaceY - previousHeight;
                int color = RadarMapColor.forMapColor(baseColor, surfaceY - playerY, slope);
                cells.add(new MapCell(screenX, screenY, color));
                previousHeight = surfaceY;
            }
        }
        return List.copyOf(cells);
    }

    private void drawMapGuides(GuiGraphicsExtractor graphics, Minecraft client, HudBounds bounds,
                               int centerX, int centerY, HudElementPlacement placement) {
        graphics.fill(centerX, bounds.y() + 1, centerX + 1, bounds.y() + bounds.height() - 1, MAP_GUIDE_COLOR);
        graphics.fill(bounds.x() + 1, centerY, bounds.x() + bounds.width() - 1, centerY + 1, MAP_GUIDE_COLOR);
        int borderColor = HudPresentation.color(placement);
        if (module.shape() == RadarProjection.Shape.SQUARE) {
            graphics.outline(bounds.x(), bounds.y(), bounds.width(), bounds.height(), borderColor);
        } else {
            for (int degrees = 0; degrees < 360; degrees += 4) {
                double radians = Math.toRadians(degrees);
                int x = centerX + (int) Math.round(Math.cos(radians) * (RADIUS - 1));
                int y = centerY + (int) Math.round(Math.sin(radians) * (RADIUS - 1));
                graphics.fill(x, y, x + 1, y + 1, borderColor);
            }
        }
        String heading = module.rotate() ? RadarProjection.heading(client.player.getYRot()) : "N";
        int headingX = centerX - client.font.width(heading) / 2;
        graphics.text(client.font, heading, headingX, bounds.y() + 2, 0xFFF4F7FA, true);
    }

    private static void drawEntityMarker(GuiGraphicsExtractor graphics, int x, int y, int color,
                                         double heightDifference) {
        graphics.fill(x - 2, y - 2, x + 3, y + 3, 0xC0000000);
        graphics.fill(x - 1, y - 1, x + 2, y + 2, color);
        if (heightDifference > 3.0D) {
            graphics.fill(x, y - 4, x + 1, y - 3, color);
        } else if (heightDifference < -3.0D) {
            graphics.fill(x, y + 3, x + 1, y + 4, color);
        }
    }

    private static void drawPlayerMarker(GuiGraphicsExtractor graphics, int centerX, int centerY,
                                         float yawDegrees, boolean rotatingMap) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        if (!rotatingMap) {
            graphics.pose().rotate((float) Math.toRadians(yawDegrees));
        }
        graphics.fill(-1, -5, 2, 4, PLAYER_OUTLINE_COLOR);
        graphics.fill(-3, 1, 4, 4, PLAYER_OUTLINE_COLOR);
        graphics.fill(0, -4, 1, 3, PLAYER_COLOR);
        graphics.fill(-2, 1, 3, 3, PLAYER_COLOR);
        graphics.pose().popMatrix();
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

    private record MapCell(int x, int y, int color) {
    }
}
